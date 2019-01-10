package edu.skku.monet.nugucall;

import java.io.Serializable;

// ThreadReceive 인터페이스 선언
public interface ThreadReceive extends Serializable {
    void onReceiveRun(String message);
}