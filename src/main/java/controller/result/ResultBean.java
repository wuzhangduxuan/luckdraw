package controller.result;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/18.
 */
public class ResultBean<T> {

    private int code;
    private T message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}
