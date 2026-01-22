package ro.tuc.virtualmemorysimulator.BusinessLogic;

import ro.tuc.virtualmemorysimulator.Model.Frame;
import java.util.List;

public interface Algorithm {
    int replacePage(List<Frame> frames, List<Integer> referenceString, int currentIndex);

    String getName();
}
