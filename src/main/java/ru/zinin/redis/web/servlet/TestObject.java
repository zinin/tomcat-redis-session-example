package ru.zinin.redis.web.servlet;

import java.io.Serializable;

/**
 * Date: 31.10.11 21:24
 *
 * @author Alexander V. Zinin (mail@zinin.ru)
 */
public class TestObject implements Serializable {
    private long time;

    public TestObject(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TestObject");
        sb.append("{time=").append(time);
        sb.append('}');
        return sb.toString();
    }
}
