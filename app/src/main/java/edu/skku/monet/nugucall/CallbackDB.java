package edu.skku.monet.nugucall;

import java.io.Serializable;

// CallbackDB 인터페이스 선언
public interface CallbackDB extends Serializable {
    void callback(String out);
}