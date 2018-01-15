package pairwiseBasedKWS;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.OutputStreamWriter;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.Map;
//
//import aqpeq.utilities.Dummy.DummyProperties;
//import graphInfra.GraphInfraReaderArray;
//import graphInfra.NodeInfra;
//
public class TwoHopCover {
//
//	final int infinity = Integer.MAX_VALUE;
//
//	ArrayList<HashMap<Integer, Integer>> distanceIndicesOfNodeId;
//
//	// array P
//	ArrayList<Integer> pOfNodeId = new ArrayList<Integer>();
//
//	HashSet<Integer> visitedNodeIds = new HashSet<Integer>();
//	ArrayList<NodeInfra> sortedNodeInfos;
//
//	GraphInfraReaderArray graphInfra;
//
//	private double startTimeGetDistance;
//
//	private double durationGetDistance = 0d;
//
//	private long startTimeBFS;
//
//	private double durationBFSNoGetDistance = 0d;
//
//	public TwoHopCover(GraphInfraReaderArray graphInfra) {
//		this.graphInfra = graphInfra;
//
//	}
//
//	// 1: procedure Preprocess(G)
//	// 2: L′0[v] ← ∅ for all v ∈ V (G).
//	// 3: for k = 1, 2, . . . , n do
//	// 4: L′k ← PrunedBFS(G, vk, L′k−1)
//	// 5: return L′n
//	public void preprocess() {
//
//		distanceIndicesOfNodeId = new ArrayList<HashMap<Integer, Integer>>(graphInfra.nodeOfNodeId.size());
//
//		for (int i = 0; i < graphInfra.nodeOfNodeId.size(); i++) {
//			distanceIndicesOfNodeId.add(null);
//			pOfNodeId.add(infinity);
//		}
//
//		/// L′0[v] ← ∅ for all v ∈ V (G).
//		for (NodeInfra node : graphInfra.nodeOfNodeId) {
//
//			distanceIndicesOfNodeId.set(node.nodeId, new HashMap<>(1));
//
//			// initializing P
//			pOfNodeId.set(node.nodeId, infinity);
//
//		}
//
//		sortedNodeInfos = new ArrayList<>(graphInfra.nodeOfNodeId);
//
//		Collections.sort(sortedNodeInfos, new Comparator<NodeInfra>() {
//
//			@Override
//			public int compare(NodeInfra o1, NodeInfra o2) {
//				if (o2.getDegree() != o1.getDegree())
//					return Integer.compare(o2.getDegree(), o1.getDegree());
//				else
//					return Integer.compare(o1.nodeId, o2.nodeId);
//			}
//		});
//
//		// for k = 1, 2, . . . , n do
//		for (NodeInfra node : sortedNodeInfos) {
//
//			if (DummyProperties.debugMode) {
//				System.out.println(node.nodeId + " deg: " + node.getDegree()
//				/*
//				 * + ", in:" + node.incomingRelIdsOfTargetNodeId + ", out:" +
//				 * node.outgoingRelIdsOfSourceNodeId
//				 */
//				);
//			}
//
//			// L′k ← PrunedBFS(G, vk, L′k−1)
//			prunedBFS(node.nodeId);
//		}
//
//		sortedNodeInfos = null;
//		pOfNodeId = null;
//		visitedNodeIds = null;
//	}
//
//	public void prunedBFS(int vNodeId) {
//
//		double localPrevGetDistanceDuration = durationGetDistance;
//
//		// Q ← a queue with only one element vk.
//		LinkedList<Integer> bfsQueue = new LinkedList<Integer>();
//
//		bfsQueue.add(vNodeId);
//
//		// P[vk] ← 0 and P[v] ← ∞ for all v ∈ V (G) \ {vk}.
//		for (Integer visitedNodeId : visitedNodeIds) {
//			pOfNodeId.set(visitedNodeId, infinity);
//		}
//		pOfNodeId.set(vNodeId, 0);
//		visitedNodeIds.clear();
//
//		int pruned = 0;
//
//		startTimeBFS = System.nanoTime();
//		// while Q is not empty do
//		while (!bfsQueue.isEmpty()) {
//
//			// Dequeue u from Q
//			Integer uNodeId = bfsQueue.poll();
//			visitedNodeIds.add(uNodeId);
//
//			// if Query(vk, u, L′k−1) ≤ P[u] then
//			if (getDistance(vNodeId, uNodeId) <= pOfNodeId.get(uNodeId)) {
//				pruned++;
//				// System.out.println("pruned: (v,u)=(" + vNodeId + "," +
//				// uNodeId + ") <= pOfNodeId.get(uNodeId):"
//				// + pOfNodeId.get(uNodeId));
//				continue;
//			}
//
//			// L′k[u] ← L′k−1[u] ∪ {(vk, P[vk])}
//
//			if (distanceIndicesOfNodeId.get(uNodeId).containsKey(vNodeId)) {
//				System.err.println("was there before maybe replaced?");
//			}
//			distanceIndicesOfNodeId.get(uNodeId).put(vNodeId, pOfNodeId.get(uNodeId));
//
//			// for all w ∈ NG(v) s.t. P[w] = ∞ do
//			if (graphInfra.nodeOfNodeId.get(uNodeId).getDegree() > 0) {
//
//				for (Integer wNodeId : graphInfra.nodeOfNodeId.get(uNodeId).getIncomingRelIdsOfTargetNodeId()
//						.keySet()) {
//					checkNodeDistanceAndAddToQueueIfNeeded(wNodeId, uNodeId, bfsQueue);
//				}
//				for (Integer wNodeId : graphInfra.nodeOfNodeId.get(uNodeId).getOutgoingRelIdsOfSourceNodeId()
//						.keySet()) {
//					checkNodeDistanceAndAddToQueueIfNeeded(wNodeId, uNodeId, bfsQueue);
//				}
//			}
//		}
//
//		durationBFSNoGetDistance += ((System.nanoTime() - startTimeBFS) / 1e6)
//				- (durationGetDistance - localPrevGetDistanceDuration);
//
//		System.out.println("pruned from vId: " + vNodeId + " deg:" + graphInfra.nodeOfNodeId.get(vNodeId).getDegree()
//				+ ", num:" + pruned);
//		System.out.println("visited nodes: " + visitedNodeIds.size());
//
//		int labelSize = 0;
//		for (int i = 0; i < distanceIndicesOfNodeId.size(); i++) {
//			labelSize += distanceIndicesOfNodeId.get(i).keySet().size();
//		}
//		System.out.println("current label size: " + labelSize);
//		System.out.println("durationGetDistance: " + durationGetDistance + " ms");
//		System.out.println("durationBFSNoGetDistance: " + durationBFSNoGetDistance + " ms");
//
//		// for (int i = 0; i < distanceIndicesOfNodeId.size(); i++) {
//		// System.out.print("v:" + i + " {");
//		// for (Integer nodeId : distanceIndicesOfNodeId.get(i).keySet()) {
//		// System.out.print("(" + nodeId + ", " +
//		// distanceIndicesOfNodeId.get(i).get(nodeId) + ")");
//		// }
//		// System.out.println("}");
//		// }
//		bfsQueue = null;
//	}
//
//	private void checkNodeDistanceAndAddToQueueIfNeeded(Integer wNodeId, Integer uNodeId,
//			LinkedList<Integer> bfsQueue) {
//		if (pOfNodeId.get(wNodeId) == infinity) {
//
//			// P[w] ← P[u] + 1.
//			pOfNodeId.set(wNodeId, pOfNodeId.get(uNodeId) + 1);
//
//			// Enqueue w onto Q.
//			bfsQueue.add(wNodeId);
//		}
//
//	}
//
//	public int getDistance(int sId, int tId) {
//		startTimeGetDistance = System.nanoTime();
//		// Query(s, t,L) =min {δvs + δvt | (v, δvs) ∈ L(s), (v, δvt) ∈ L(t)}
//		int minValue = infinity;
//
//		if (distanceIndicesOfNodeId.get(sId) != null && distanceIndicesOfNodeId.get(tId) != null) {
//
//			for (Integer id : distanceIndicesOfNodeId.get(sId).keySet()) {
//				if (distanceIndicesOfNodeId.get(tId).containsKey(id)) {
//					int distance = distanceIndicesOfNodeId.get(sId).get(id)
//							+ distanceIndicesOfNodeId.get(tId).get(id);
//					minValue = Math.min(distance, minValue);
//				}
//			}
//
//		}
//
//		durationGetDistance += ((System.nanoTime() - startTimeGetDistance) / 1e6);
//		return minValue;
//	}
//
//	public void print() throws Exception {
//
//		File fout = new File("landmarkLabeling.txt");
//		FileOutputStream fos = new FileOutputStream(fout);
//		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//
//		for (int i = 0; i < distanceIndicesOfNodeId.size(); i++) {
//			bw.write(i + "#");
//			Map<Integer, Integer> nodeIndex = distanceIndicesOfNodeId.get(i);
//			for (Integer nodeId : nodeIndex.keySet()) {
//				bw.write(nodeId + ":" + nodeIndex.get(nodeId) + ";");
//			}
//			bw.newLine();
//		}
//		bw.close();
//
//	}
//
//	public static void main(String[] args) throws Exception {
//
//		DummyProperties.debugMode = false;
//		// String dgPath =
//		// "/Users/mnamaki/Documents/Education/PhD/Summer2017/AQEQ/Datasets/amazon/amazonGraphInfra/";
//		// String dgPath =
//		// "/Users/mnamaki/Documents/Education/PhD/Summer2017/AQEQ/Datasets/Synthetics/n15/";
//		// String dgPath =
//		// "/Users/mnamaki/Documents/Education/PhD/Summer2017/AQEQ/Datasets/Synthetics/distanceExmaple/";
//		String dgPath = args[0];
//
//		GraphInfraReaderArray graph = new GraphInfraReaderArray(dgPath);
//		graph.read();
//
//		// for (NodeInfra node : graph.nodeOfNodeId) {
//		// System.out.println(node.nodeId + " => in:" +
//		// node.getIncomingRelIdsOfTargetNodeId() + " out:"
//		// + node.getOutgoingRelIdsOfSourceNodeId());
//		// }
//
//		TwoHopCover thc = new TwoHopCover(graph);
//		thc.preprocess();
//		thc.print();
//
//		System.out.println("finish");
//		// amazon
//		// System.out.println("1568250,1568250=>" + thc.getDistance(1568250,
//		// 1568250));
//		// System.out.println("212192,1568250=>" + thc.getDistance(212192,
//		// 1568250));
//		// System.out.println("1568250, 212192=>" + thc.getDistance(1568250,
//		// 212192));
//		// System.out.println("1328535, 1745572=>" + thc.getDistance(1328535,
//		// 1745572));
//		// System.out.println("2085911, 1328535" + thc.getDistance(2085911,
//		// 1328535));
//		// System.out.println("1328535, 2085911=>" + thc.getDistance(1328535,
//		// 2085911));
//
//		// n15
//		// System.out.println("4,13=>" + thc.getDistance(4, 13));
//		// System.out.println("4,4=>" + thc.getDistance(4, 4));
//		// System.out.println("5, 4=>" + thc.getDistance(5, 4));
//		// System.out.println("5, 14=>" + thc.getDistance(5, 14));
//
//		// original distanceExample
//		// System.out.println("8,8=>" + thc.getDistance(8, 8));
//		// System.out.println("8,2=>" + thc.getDistance(8, 2));
//		// System.out.println("2,8=>" + thc.getDistance(2, 8));
//		// System.out.println("1,11=>" + thc.getDistance(1, 11));
//		// System.out.println("4,3=>" + thc.getDistance(4, 3));
//
//	}
}