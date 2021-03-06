//TODO: add current keywords to make sure we do not get them again!

package queryExpansion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import aqpeq.utilities.KWSUtilities;
import aqpeq.utilities.Dummy.DummyFunctions;
import aqpeq.utilities.Dummy.DummyProperties;
import bicliqueResearch.Tuple;
import bicliqueResearch.TupleComparator;
import graphInfra.GraphInfraReaderArray;
import graphInfra.NodeInfra;

public class RootKWSExpand {

	// top-n answer trees A discovered by KWS algorithm A,
	public ArrayList<AnswerAsInput> topNAnswers;

	// quality bound
	double delta;

	// quality preservable keywords K ={(k′, w′), . . . }.
	public HashMap<Integer, CostAndNodesOfAnswersPair> estimatedWeightOfSuggestedKeywordMap;

	// augmented data graph
	GraphInfraReaderArray graph;

	// kCost
	HashMap<Integer, CostNodePair[]> costOfAKeywordToAnAnswer;

	// top-n answer
	public HashMap<Integer, HashMap<Integer, Double>> answerKeywordMap;

	public double w;
	private int b;

	public int visitedNodes = 0;
	public int visitedKeywords = 0;
	public double avgQualityOfSuggestedKeyword = 0d;
	public double initialOveralWeight = 0d;
	public double querySuggestionKWSStartTime = 0d;
	public double querySuggestionKWSDuration = 0d;

	// after pruning high-frequent keywordsF
	public double totalWeightOfSuggestedKeywords = 0d;
	public double lowestWeightOfSuggestedKeyword;
	public Integer lowestWeightSuggestedKeywordId;

	public int highFrequentKeywordsRemovedNum = 0;

	public CostAndNodesOfAnswersPair bestKeywordInfo;

	public HashSet<Integer> keywordsSet;

	public double getKeywordsDuration = 0d;

	// FOR TEST
	// public boolean testSizeOfGrVsExploredArea = true;
	// public int totalNumberOfNodesVisitedInGr = 0;
	// public int totalNumberOfEdgesVisitedInGr = 0;
	// public int totalNumberOfEdgesVisitedInExplore = 0;
	// public int totalNumberOfNodesVisitedInExplore = 0;
	// public HashSet<Integer> Q_G_DELTA_Q_G_Nodes = new HashSet<Integer>();
	// public HashSet<Integer> Q_G_DELTA_Q_G_Edges = new HashSet<Integer>();
	// public HashSet<Integer> G_r_Nodes = new HashSet<Integer>();
	// public HashSet<Integer> G_r_Edges = new HashSet<Integer>();

	public RootKWSExpand(GraphInfraReaderArray graph, ArrayList<AnswerAsInput> topNAnswers, double delta, int b,
			HashSet<Integer> keywordsSet) {
		this.topNAnswers = topNAnswers;
		this.delta = delta;
		this.graph = graph;

		this.keywordsSet = keywordsSet;

		for (int m = 0; m < topNAnswers.size(); m++) {
			this.w += topNAnswers.get(m).getCost();
		}

		initialOveralWeight = this.w;

		this.b = b;
	}

	public HashMap<Integer, CostAndNodesOfAnswersPair> expand() throws Exception {

		estimatedWeightOfSuggestedKeywordMap = new HashMap<Integer, CostAndNodesOfAnswersPair>();
		costOfAKeywordToAnAnswer = new HashMap<Integer, CostNodePair[]>();

		int n = topNAnswers.size();

		querySuggestionKWSStartTime = System.nanoTime();

		// for i := 1 to n do
		for (int i = 0; i < n; i++) {

			// queue L := ∅, set visited nodes S := ∅;
			LinkedList<BFSTriple> queue = new LinkedList<BFSTriple>();
			HashSet<Integer> visitedNodesSet = new HashSet<Integer>();
			// HashSet<Integer> visitedEdgesSet = new HashSet<Integer>();

			// L := L ∪ ⟨ri , 0, 0⟩; /* information node ri in answer ai */
			queue.add(new BFSTriple(topNAnswers.get(i).getRootNodeId(), 0, 0d));

			int tempMaxVisit = DummyProperties.MaxNumberOfVisitedNodes;

			// while (L , ∅) do
			while (!queue.isEmpty()) {
				/* Picking the next node in a FIFO fashion */
				// ⟨v, d, c⟩ := L.poll ();
				BFSTriple currentBFSTriple = queue.poll();

				int v = currentBFSTriple.getNodeId();
				int d = currentBFSTriple.getDistance();
				double c = currentBFSTriple.getCost();

				// S = S ∪ {v };
				visitedNodesSet.add(v);

				// not root itself
				if (v != topNAnswers.get(i).getRootNodeId()) {
					// for each keyword k′ in v.getKeywords () do

					double startTime = System.nanoTime();
					Collection<Integer> keywordsOfV = DummyFunctions.getKeywords(graph, v);
					getKeywordsDuration += (System.nanoTime() - startTime) / 1e6;

					if (keywordsOfV == null) {
						keywordsOfV = new HashSet<Integer>();
					}

					for (Integer k_ : keywordsOfV) {

						// if i > 1 and k′ < kCost then continue
						if (i > 0 && !costOfAKeywordToAnAnswer.containsKey(k_))
							continue;

						if (keywordsSet.contains(k_))
							continue;

						// kCost [k′][ai ] := min(kCost [k′][ai ], c );
						// if (DummyProperties.debugMode)
						// System.out.println("k_ " + k_);

						if (!costOfAKeywordToAnAnswer.containsKey(k_)) {
							costOfAKeywordToAnAnswer.put(k_, new CostNodePair[n]);
							CostNodePair[] arr = costOfAKeywordToAnAnswer.get(k_);
							for (int o = 0; o < arr.length; o++) {
								arr[o] = new CostNodePair(-1, new InfiniteDouble());
							}
						}

						if (costOfAKeywordToAnAnswer.get(k_)[i].cost.getValue() > c) {
							costOfAKeywordToAnAnswer.get(k_)[i].cost.setValue(c);
							costOfAKeywordToAnAnswer.get(k_)[i].nodeId = v;
						}

						/* Last BFS */
						// if i = n then
						if (i == n - 1) {
							// w′ :=\sum i=1 to n kCost [k′][ai];
							double w_ = QueryExpandUtility.getSumOfCosts(costOfAKeywordToAnAnswer.get(k_));

							// if w′ ≤ δ ∗ w then
							if (w_ <= delta * w) {

								int[] nodeIds = QueryExpandUtility.getNodeIdArr(costOfAKeywordToAnAnswer.get(k_));

								// if (k_.contains("aspe_2004")) {
								// System.out.println();
								// }

								// K := K ∪ (k′, w + w′);
								if (estimatedWeightOfSuggestedKeywordMap.containsKey(k_)) {
									if (estimatedWeightOfSuggestedKeywordMap.get(k_).cost > w_ + w) {

										estimatedWeightOfSuggestedKeywordMap.put(k_,
												new CostAndNodesOfAnswersPair(nodeIds, w_ + w));
									}
								} else {
									estimatedWeightOfSuggestedKeywordMap.put(k_,
											new CostAndNodesOfAnswersPair(nodeIds, w_ + w));
								}

							}

						}

					}
				}
				// if d ≥ b then continue
				if (d >= b)
					continue;

				// to take care of high-degree
				if (queue.size() > tempMaxVisit) {
					tempMaxVisit--;
					continue;
				}

				// for each each edge e = (u, v) in G′ do
				for (int targetNodeId : graph.nodeOfNodeId.get(v).getOutgoingRelIdOfSourceNodeId().keySet()) {

					// if (testSizeOfGrVsExploredArea)
					// visitedEdgesSet
					// .add(graph.nodeOfNodeId.get(v).getOutgoingRelIdOfSourceNodeId().get(targetNodeId));

					// if u ∈ S then continue
					if (visitedNodesSet.contains(targetNodeId))
						continue;

					// c′ := c + w_e
					double c_ = c + graph.relationOfRelId
							.get(graph.nodeOfNodeId.get(targetNodeId).getOutgoingRelIdOfSourceNodeId().get(v)).weight;

					// if w′ > δ ∗ w then continue
					if (c_ > delta * w)
						continue;

					// L := L ∪ ⟨u, d + 1, c′⟩
					queue.add(new BFSTriple(targetNodeId, d + 1, c_));
				}

			}

			visitedNodes += visitedNodesSet.size();

			if (DummyProperties.debugMode) {
				System.out.println("visited nodes so far: " + visitedNodes);
				System.out.println("visited keywords so far:" + estimatedWeightOfSuggestedKeywordMap.keySet().size());
			}

			// if (testSizeOfGrVsExploredArea) {
			// totalNumberOfEdgesVisitedInExplore += visitedEdgesSet.size();
			// totalNumberOfEdgesVisitedInExplore += visitedNodesSet.size();
			//
			// Q_G_DELTA_Q_G_Nodes.addAll(visitedNodesSet);
			// Q_G_DELTA_Q_G_Edges.addAll(visitedEdgesSet);
			// }

		}

		querySuggestionKWSDuration = ((System.nanoTime() - querySuggestionKWSStartTime) / 1e6);

		visitedKeywords = costOfAKeywordToAnAnswer.size();

		int tempNum = estimatedWeightOfSuggestedKeywordMap.size();

		KWSUtilities.removeHighFrequentKeywordsFromMap(graph, estimatedWeightOfSuggestedKeywordMap);

		highFrequentKeywordsRemovedNum = tempNum - estimatedWeightOfSuggestedKeywordMap.size();

		totalWeightOfSuggestedKeywords = 0d;
		lowestWeightOfSuggestedKeyword = Double.MAX_VALUE;
		lowestWeightSuggestedKeywordId = 0;
		if (estimatedWeightOfSuggestedKeywordMap.size() > 0) {

			for (Integer k : estimatedWeightOfSuggestedKeywordMap.keySet()) {
				totalWeightOfSuggestedKeywords += estimatedWeightOfSuggestedKeywordMap.get(k).cost;

				if (lowestWeightOfSuggestedKeyword > estimatedWeightOfSuggestedKeywordMap.get(k).cost) {
					lowestWeightOfSuggestedKeyword = estimatedWeightOfSuggestedKeywordMap.get(k).cost;
					lowestWeightSuggestedKeywordId = k;
				}

			}

			avgQualityOfSuggestedKeyword = totalWeightOfSuggestedKeywords
					/ (double) estimatedWeightOfSuggestedKeywordMap.size();

		}

		bestKeywordInfo = estimatedWeightOfSuggestedKeywordMap.get(lowestWeightSuggestedKeywordId);

		return estimatedWeightOfSuggestedKeywordMap;
	}

	// output: answer reach to keyword
	public void expandForBiClique() throws Exception {
		// key: answer id, value: Map(keyword, length)
		answerKeywordMap = new HashMap<Integer, HashMap<Integer, Double>>();

		for (int i = 0; i < topNAnswers.size(); i++) {

			int rootId = topNAnswers.get(i).getRootNodeId();

			HashMap<Integer, Double> keywordDistance = new HashMap<Integer, Double>();

			// generate sssp from root node of current answer
			SSSPIterator sssp = new SSSPIterator(graph, rootId, b);
			HashSet<Integer> visitedNodesSet = new HashSet<Integer>();
			visitedNodesSet.add(rootId);

			// extract keywords in root node
			HashSet<Integer> rootKeywords = DummyFunctions.getKeywords(graph, rootId);
			for (int keywordId : rootKeywords) {
				keywordDistance.putIfAbsent(keywordId, 0.0);
			}

			while (hasNextNode(sssp)) {
				SSSPNode currentNode = sssp.getNextSSSPNode();
				NodeInfra curNode = currentNode.node;
				if (!visitedNodesSet.contains(curNode.nodeId)) {
					double distanceFromOriginId = currentNode.distanceFromOriginId;
					// extract keywords in current node
					HashSet<Integer> currentKeywords = DummyFunctions.getKeywords(graph, curNode.nodeId);
					
					//put them with distance
					for (int keywordId : currentKeywords) {
						if (!keywordsSet.contains(keywordId)){
							keywordDistance.putIfAbsent(keywordId, distanceFromOriginId);
						}
					}
					visitedNodesSet.add(curNode.nodeId);
				}
			}

			answerKeywordMap.put(i, keywordDistance);
		}

	}

	public boolean hasNextNode(SSSPIterator sssp) {
		if (sssp.peekDist() <= b) {
			return true;
		}

		return false;
	}

	// public void updateNumberOfNodesAndEdgesInGr() throws Exception {
	//
	// if (!testSizeOfGrVsExploredArea)
	// return;
	//
	// int n = topNAnswers.size();
	//
	// // for i := 1 to n do
	// for (int i = 0; i < n; i++) {
	//
	// // queue L := ∅, set visited nodes S := ∅;
	// LinkedList<BFSTriple> queue = new LinkedList<BFSTriple>();
	// HashSet<Integer> visitedNodesSet = new HashSet<Integer>();
	// HashSet<Integer> visitedEdgesSet = new HashSet<Integer>();
	//
	// // L := L ∪ ⟨ri , 0, 0⟩; /* information node ri in answer ai */
	// queue.add(new BFSTriple(topNAnswers.get(i).getRootNodeId(), 0, 0d));
	//
	// // while (L , ∅) do
	// while (!queue.isEmpty()) {
	// /* Picking the next node in a FIFO fashion */
	// // ⟨v, d, c⟩ := L.poll ();
	// BFSTriple currentBFSTriple = queue.poll();
	//
	// int v = currentBFSTriple.getNodeId();
	// int d = currentBFSTriple.getDistance();
	// double c = currentBFSTriple.getCost();
	//
	// // S = S ∪ {v };
	// visitedNodesSet.add(v);
	//
	// // if d ≥ b then continue
	// if (d >= b)
	// continue;
	//
	// // to take care of high-degree
	// // if (queue.size() > DummyProperties.MaxNumberOfVisitedNodes)
	// // continue;
	//
	// // for each each edge e = (u, v) in G′ do
	// for (int targetNodeId :
	// graph.nodeOfNodeId.get(v).getOutgoingRelIdOfSourceNodeId().keySet()) {
	//
	// int relID =
	// graph.nodeOfNodeId.get(v).getOutgoingRelIdOfSourceNodeId().get(targetNodeId);
	//
	// visitedEdgesSet.add(relID);
	//
	// // if u ∈ S then continue
	// if (visitedNodesSet.contains(targetNodeId))
	// continue;
	//
	// // L := L ∪ ⟨u, d + 1, c′⟩
	// queue.add(new BFSTriple(targetNodeId, d + 1, 0));
	// }
	//
	// }
	//
	// totalNumberOfNodesVisitedInGr += visitedNodesSet.size();
	// totalNumberOfEdgesVisitedInGr += visitedEdgesSet.size();
	// G_r_Nodes.addAll(visitedNodesSet);
	// G_r_Edges.addAll(visitedEdgesSet);
	//
	// }
	//
	// }
}
