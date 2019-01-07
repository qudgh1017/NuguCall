package edu.skku.monet.nugucall;

import java.io.Serializable;

public interface CallbackDB extends Serializable {
    void callback(String out);
}