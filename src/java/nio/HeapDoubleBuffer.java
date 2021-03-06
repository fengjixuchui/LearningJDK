/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio;

/**
 * A read/write HeapDoubleBuffer.
 */
// 可读写、非直接缓冲区，内部存储结构实现为double[]
class HeapDoubleBuffer extends DoubleBuffer {
    
    // Cached array base offset
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(double[].class);
    // Cached array base offset
    private static final long ARRAY_INDEX_SCALE = UNSAFE.arrayIndexScale(double[].class);
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    protected HeapDoubleBuffer(double[] buf, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap, buf, off);
        this.address = ARRAY_BASE_OFFSET + off * ARRAY_INDEX_SCALE;
    }
    
    HeapDoubleBuffer(int cap, int lim) {            // package-private
        super(-1, 0, lim, cap, new double[cap], 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    HeapDoubleBuffer(double[] buf, int off, int len) { // package-private
        super(-1, off, off + len, buf.length, buf, 0);
        this.address = ARRAY_BASE_OFFSET;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 只读/可读写
    public boolean isReadOnly() {
        return false;
    }
    
    // 直接缓冲区/非直接缓冲区
    public boolean isDirect() {
        return false;
    }
    
    /*▲ 可读写/非直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public DoubleBuffer slice() {
        return new HeapDoubleBuffer(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public DoubleBuffer duplicate() {
        return new HeapDoubleBuffer(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public DoubleBuffer asReadOnlyBuffer() {
        return new HeapDoubleBufferR(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get/读取 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position处（可能需要加offset）的double，然后递增position。
    public double get() {
        return hb[ix(nextGetIndex())];
    }
    
    // 读取i处（可能需要加offset）的double（有越界检查）
    public double get(int i) {
        return hb[ix(checkIndex(i))];
    }
    
    // 复制源缓存区的length个元素到dst数组offset索引处
    public DoubleBuffer get(double[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if(length>remaining())
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(position()), dst, offset, length);
        position(position() + length);
        return this;
    }
    
    /*▲ get/读取 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put/写入 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position处（可能需要加offset）写入double，并将position递增
    public DoubleBuffer put(double x) {
        hb[ix(nextPutIndex())] = x;
        return this;
    }
    
    // 向i处（可能需要加offset）写入double
    public DoubleBuffer put(int i, double x) {
        hb[ix(checkIndex(i))] = x;
        return this;
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public DoubleBuffer put(double[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if(length>remaining())
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(position()), length);
        position(position() + length);
        return this;
    }
    
    // 将double数组src的全部内容写入此缓冲区
    public DoubleBuffer put(DoubleBuffer src) {
        if(src instanceof HeapDoubleBuffer) {
            if(src == this)
                throw createSameBufferException();
            HeapDoubleBuffer sb = (HeapDoubleBuffer) src;
            int n = sb.remaining();
            if(n>remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()), hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if(src.isDirect()) {
            int n = src.remaining();
            if(n>remaining())
                throw new BufferOverflowException();
            src.get(hb, ix(position()), n);
            position(position() + n);
        } else {
            super.put(src);
        }
        return this;
    }
    
    /*▲ put/写入 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public DoubleBuffer compact() {
        System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回该缓冲区的字节序（大端还是小端）
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    protected int ix(int i) {
        return i + offset;
    }
}
