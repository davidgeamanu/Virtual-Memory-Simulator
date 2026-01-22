package ro.tuc.virtualmemorysimulator.BusinessLogic;

import ro.tuc.virtualmemorysimulator.Model.Frame;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FIFOAlgorithm implements Algorithm {
    private final Queue<Integer> frameQueue;

    public FIFOAlgorithm() {
        frameQueue = new LinkedList<>();
    }

    @Override
    public int replacePage(List<Frame> frames, List<Integer> referenceString, int currentIndex) {
        int frameToReplace = frameQueue.poll();
        frameQueue.add(frameToReplace);
        return frameToReplace;
    }


    public void addFrame(int frameId) {
        frameQueue.add(frameId);
    }

    @Override
    public String getName() {
        return "FIFO";
    }
}
