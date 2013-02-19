/*
 *     Copyright 2012 Couchbase, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package gson_ext;

import java.io.IOException;

public class RingBuffer {

    char[] data;
    int datalen = 0;
    int rh = 0;
    int wh = 0;
    Reader reader;
    Writer writer;
    java.io.Reader alien = null;
    boolean chunked = false;

    public RingBuffer(java.io.Reader externalSource, int capacity) {
        alien = externalSource;
        data = new char[capacity];
        reader = new Reader(this);
        writer = new Writer(this);
    }

    public RingBuffer(java.io.Reader externalSource) {
        this(externalSource, 16);
    }

    public RingBuffer(int capacity) {
        this(null, capacity);
    }

    public RingBuffer() {
        this(null, 16);
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public boolean isChunked() {
        return this.chunked;
    }

    public void setExternalSource(java.io.Reader externalSource) {
        if (alien != null) {
            try {
                /* FIXME read out before close it */
                alien.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        alien = externalSource;
    }

    public void write(char[] src, int len) {
        ensureCapacity(len);
        if (wh + len < data.length) {
            System.arraycopy(src, 0, data, wh, len);
            wh += len;
        } else {
            int rest = data.length - wh;
            System.arraycopy(src, 0, data, wh, rest);
            wh = len - rest;
            System.arraycopy(src, rest, data, 0, wh);
        }
        datalen += len;
    }

    public void write(char[] src) {
        write(src, src.length);
    }

    public int read(char cbuf[], int off, int len) throws IOException {
        if (alien != null && datalen < len) {
            char[] input = new char[len];
            int nb = alien.read(input);
            if (nb == -1) {
                alien = null;
            } else {
                write(input, nb);
            }
        }
        int size = Math.min(datalen, len);
        if (size > 0) {
            if (rh + size < data.length) {
                System.arraycopy(data, rh, cbuf, off, size);
                rh += size;
            } else {
                int rest = data.length - rh;
                System.arraycopy(data, rh, cbuf, off, rest);
                rh = size - rest;
                System.arraycopy(data, 0, cbuf, rest, rh);
            }
            datalen -= size;
            return size;
        } else {
            return -1;
        }
    }

    private void ensureCapacity(int capacity) {
        if (data.length - datalen < capacity) {
            int new_size = data.length * 2;
            while (new_size - datalen <= capacity) {
                new_size *= 2;
            }
            char[] tmp = new char[new_size];
            if (rh < wh) {
                System.arraycopy(data, rh, tmp, 0, wh - rh);
            } else {
                int len = data.length - rh;
                System.arraycopy(data, rh, tmp, 0, len);
                System.arraycopy(data, 0, tmp, len, wh);
            }
            data = tmp;
            wh = datalen;
            rh = 0;
        }
    }

    class Reader extends java.io.Reader {
        RingBuffer buff;

        public Reader(RingBuffer buff) {
            this.buff = buff;
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            synchronized (lock) {
                ensureOpen();
                return buff.read(cbuf, off, len);
            }
        }

        public long skip(long n) throws IOException {
            synchronized (lock) {
                ensureOpen();
                n = Math.min(buff.datalen, n);
                buff.rh = (int)((buff.rh + n) % buff.data.length);
                return n;
            }
        }

        public boolean ready() throws IOException {
            synchronized (lock) {
                ensureOpen();
                return true;
            }
        }

        public boolean markSupported() {
            return false;
        }

        public void close() {
            buff = null;
        }

        private void ensureOpen() throws IOException {
            if (buff == null) {
                throw new IOException("Stream closed");
            }
        }
    }

    class Writer extends java.io.Writer {
        RingBuffer buff;

        public Writer(RingBuffer buff) {
            this.buff = buff;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            synchronized (lock) {
                ensureOpen();
                char[] src;
                if (off == 0 && len == cbuf.length) {
                    src = cbuf;
                } else {
                    src = new char[len];
                    System.arraycopy(cbuf, off, src, 0, len);
                }
                buff.write(src);
            }
        }

        public void flush() {
        }

        public void close() {
            buff = null;
        }

        private void ensureOpen() throws IOException {
            if (buff == null) {
                throw new IOException("Stream closed");
            }
        }
    }

}
