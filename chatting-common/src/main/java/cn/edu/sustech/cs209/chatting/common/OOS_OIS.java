package cn.edu.sustech.cs209.chatting.common;

import java.io.*;

public class OOS_OIS {
    public static class MyObjectOutputStream extends ObjectOutputStream {

        public MyObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            //重写读取头部信息方法：不写入头部信息
            super.reset();
        }
    }

    public static class MyObjectInputStream extends ObjectInputStream {
        public MyObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected void readStreamHeader() throws IOException {
            //重写读取头部信息方法：什么也不做
        }
    }
}
