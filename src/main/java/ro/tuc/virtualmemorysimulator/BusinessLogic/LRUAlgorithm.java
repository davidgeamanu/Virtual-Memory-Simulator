package ro.tuc.virtualmemorysimulator.BusinessLogic;

import ro.tuc.virtualmemorysimulator.Model.Frame;
import ro.tuc.virtualmemorysimulator.Model.Page;

import java.time.LocalDateTime;
import java.util.List;

public class LRUAlgorithm implements Algorithm{

    @Override
    public int replacePage(List<Frame> frames, List<Integer> referenceString, int currentIndex) {
        LocalDateTime oldestTime = LocalDateTime.now();
        int indexToReplace = 0;

        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            Page page = frame.getCurrentPage();
            if (page != null && page.getLastUsedTime().isBefore(oldestTime)) {
                oldestTime = page.getLastUsedTime();
                indexToReplace = i;
            }
        }
        return indexToReplace;
    }

    @Override
    public String getName() {
        return "LRU";
    }
}
