package info.itline.jrecode;

import static info.itline.jrecode.Const.*;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Encoder implements Flushable, Closeable {
	
	public Encoder(OutputStream s) {
		mOut = new DataOutputStream(s);
	}
	
	public Encoder encodeNull() throws IOException {
		mOut.writeByte(CHR_NONE);
		return this;
	}
	
	public Encoder encode(boolean b) throws IOException {
		mOut.writeByte(b ? CHR_TRUE : CHR_FALSE);
		return this;
	}
	
	public Encoder encode(byte b) throws IOException {
		if (0 <= b && b < INT_POS_FIXED_COUNT) {
			mOut.writeByte(INT_POS_FIXED_START + b);
		}
		else if (-INT_NEG_FIXED_COUNT <= b && b < 0) {
			mOut.writeByte(INT_NEG_FIXED_START - 1 - b);
		}
		else {
			mOut.writeByte(CHR_INT1);
			mOut.writeByte(b);
		}
		return this;
	}
	
	public Encoder encode(short s) throws IOException {
		mOut.writeByte(CHR_INT2);
		mOut.writeShort(s);
		return this;
	}
	
	public Encoder encode(int i) throws IOException {
		mOut.writeByte(CHR_INT4);
		mOut.writeInt(i);
		return this;
	}
	
	public Encoder encode(long l) throws IOException {
		mOut.writeByte(CHR_INT8);
		mOut.writeLong(l);
		return this;
	}
	
	public Encoder encode(BigInteger i) throws IOException {
		byte[] b = i.toString().getBytes("US-ASCII");
		if (b.length > MAX_INT_LENGTH) {
			throw new IllegalArgumentException("BigInteger value is too large");
		}
		mOut.writeByte(CHR_INT);
		mOut.write(b);
		mOut.write(CHR_TERM);
		return this;
	}
	
	public void encode(float f) throws IOException {
		mOut.writeByte(CHR_FLOAT32);
		mOut.writeFloat(f);
	}
	
	public void encode(double d) throws IOException {
		mOut.writeByte(CHR_FLOAT64);
		mOut.writeDouble(d);
	}
	
	public Encoder encode(String s) throws IOException {
		byte[] b = s.getBytes(mUseUtf ? "UTF-8" : "US-ASCII");
		if (b.length < STR_FIXED_COUNT) {
			mOut.writeByte((byte) (STR_FIXED_START + b.length));
			mOut.write(b);
		}
		else {
			mOut.write(Integer.toString(s.length()).getBytes("US-ASCII"));
			mOut.writeByte(STR_LEN_DATA_SEPARATOR);
			mOut.write(b);
		}
		return this;
	}
	
	public Encoder encode(List<Object> t) throws IOException {
		int size = t.size();
		if (size < LIST_FIXED_COUNT) {
			mOut.writeByte(LIST_FIXED_START + size); 
			for (Object o: t) {
				encodeObject(o);
			}
		}
		else {
			mOut.writeByte(CHR_LIST);
			for (Object o: t) {
				encodeObject(o);
			}
			mOut.writeByte(CHR_TERM);
		}
		return this;
	}
	
	public Encoder encode(Map<Object, Object> m) throws IOException {
		int size = m.size();
		if (size < DICT_FIXED_COUNT) {
			mOut.writeByte(DICT_FIXED_START + size);
			for (Map.Entry<Object, Object> e: m.entrySet()) {
				encodeObject(e.getKey()).encodeObject(e.getValue());
			}
		}
		else {
			mOut.writeByte(CHR_DICT);
			for (Map.Entry<Object, Object> e: m.entrySet()) {
				encodeObject(e.getKey()).encodeObject(e.getValue());
			}
			mOut.writeByte(CHR_TERM);
		}
		return this;
	}
	
	public Encoder encodeObject(Object o) throws IOException {
		if (o == null) {
			encodeNull();
		}
		else if (o instanceof Boolean) {
			encode((boolean) o);
		}
		else if (o instanceof Byte) {
			encode((byte) o);
		}
		else if (o instanceof Short) {
			encode((short) o);
		}
		else if (o instanceof Integer) {
			encode((int) o);
		}
		else if (o instanceof Long) {
			encode((long) o);
		}
		else if (o instanceof BigInteger) {
			encode((BigInteger) o);
		}
		else if (o instanceof Float) {
			encode((float) o);
		}
		else if (o instanceof Double) {
			encode((double) o);
		}
		else if (o instanceof String) {
			encode((String) o);
		}
		else if (o instanceof Map) {
			encode((Map<Object, Object>) o);
		}
		else if (o instanceof List) {
			encode((List<Object>) o);
		}
		else {
			throw new IllegalArgumentException("Object is not serializable: " + o.getClass());
		}
		return this;
	}
	
	public void close() throws IOException {
		mOut.close();
	}
	
	public void flush() throws IOException {
		mOut.flush();
	}
	
	public boolean isUseUtf() {
		return mUseUtf;
	}

	public void setUseUtf(boolean useUtf) {
		mUseUtf = useUtf;
	}

	private DataOutputStream mOut;
	private boolean mUseUtf = true;
}
