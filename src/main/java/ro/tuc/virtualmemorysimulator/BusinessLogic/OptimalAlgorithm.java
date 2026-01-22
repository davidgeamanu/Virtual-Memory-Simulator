package ro.tuc.virtualmemorysimulator.BusinessLogic;

import ro.tuc.virtualmemorysimulator.Model.Frame;

import java.util.List;

public class OptimalAlgorithm implements Algorithm{

    @Override
    public int replacePage(List<Frame> frames, List<Integer> referenceString, int currentIndex) {
        int farthestIndex = currentIndex;
        int frameToReplace = 0;

        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            int pageId = frame.getCurrentPage().getPageId();
            boolean foundLater = false;

            for (int j = currentIndex + 1; j < referenceString.size(); j++) {
                if (referenceString.get(j) == pageId) {
                    foundLater = true;
                    if (j > farthestIndex) {
                        farthestIndex = j;
                        frameToReplace = i;
                    }
                    break;
                }
            }

            if (!foundLater) {
                return i;
            }
        }
        return frameToReplace;
    }

    @Override
    public String getName() {
        return "Optimal";
    }
}
