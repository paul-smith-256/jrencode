package info.itline.jrecode;

import static info.itline.jrecode.Const.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Decoder {
	
	public Decoder(InputStream s) {
		mIn = new DataInputStream(s);
	}
	
	public Object decode() throws IOException {
		return decode(mIn.readByte() & 0xFF);
	}
	
	private Object decode(int t) throws IOException {
		if (t == CHR_NONE) {
			return null;
		}
		else if (t == CHR_FALSE) {
			return false;
		}
		else if (t == CHR_TRUE) {
			return true;
		}
		else if (t == CHR_INT1) {
			return mIn.readByte();
		}
		else if (t == CHR_INT2) {
			return mIn.readShort();
		}
		else if (t == CHR_INT4) {
			return mIn.readInt();
		}
		else if (t == CHR_INT8) {
			return mIn.readLong();
		}
		else if (t == CHR_FLOAT32) {
			return mIn.readFloat();
		}
		else if (t == CHR_FLOAT64) {
			return mIn.readDouble();
		}
		else if (t == CHR_INT) {
			byte[] b = readWhile((byte) CHR_TERM);
			return new BigInteger(new String(b, "US-ASCII"));
		}
		else if ((int) '0' <= t && t <= (int) '9') {
			byte[] tmp = readWhile(STR_LEN_DATA_SEPARATOR);
			byte[] lenBytes = new byte[tmp.length + 1];
			lenBytes[0] = (byte) t;
			System.arraycopy(tmp, 0, lenBytes, 1, tmp.length);
			int len;
			try {
				len = Integer.parseInt(new String(lenBytes, "US-ASCII"));
			}
			catch (NumberFormatException e) {
				throw new IOException(e.getMessage());
			}
			byte[] data = new byte[len];
			int bytesRead = mIn.read(data);
			if (bytesRead != len) {
				throw new EOFException();
			}
			return new String(data, mUseUtf ? "UTF-8" : "US-ASCII");
		}
		else if (t == CHR_LIST) {
			List<Object> result = new ArrayList<Object>();
			int t1 = mIn.readByte() & 0xFF;
			while (t1 != CHR_TERM) {
				result.add(decode(t1));
				t1 = mIn.readByte() & 0xFF;
			}
			return result;
		}
		else if (t == CHR_DICT) {
			Map<Object, Object> result = new HashMap<Object, Object>();
			int t1 = mIn.readByte() & 0xFF;
			while (t1 != CHR_TERM) {
				Object key = decode(t1);
				t1 = mIn.readByte() & 0xFF;
				Object value = decode(t1);
				t1 = mIn.readByte() & 0xFF;
			}
			return result;
		}
		else if (INT_POS_FIXED_START <= t && t < INT_POS_FIXED_START + INT_POS_FIXED_COUNT) {
			return t - INT_POS_FIXED_START;
		}
		else if (INT_NEG_FIXED_START <= t && t < INT_NEG_FIXED_START + INT_NEG_FIXED_COUNT) {
			return INT_NEG_FIXED_START - t - 1;
		}
		else if (STR_FIXED_START <= t && t < STR_FIXED_START + STR_FIXED_COUNT) {
			int len = t - STR_FIXED_START;
			byte[] b = new byte[len];
			int bytesRead = mIn.read(b);
			if (bytesRead != len) {
				throw new EOFException();
			}
			return new String(b, mUseUtf ? "UTF-8" : "US-ASCII");
		}
		else if (LIST_FIXED_START <= t && t < LIST_FIXED_START + LIST_FIXED_COUNT) {
			List<Object> result = new ArrayList<Object>();
			for (int i = 0; i < t - LIST_FIXED_START; i++) {
				int t1 = mIn.readByte() & 0xFF;
				result.add(decode(t1));
			}
			return result;
		}
		else if (DICT_FIXED_START <= t && t < DICT_FIXED_START + DICT_FIXED_COUNT) {
			Map<Object, Object> result = new HashMap<Object, Object>();
			for (int i = 0; i < t - DICT_FIXED_START; i++) {
				int t1 = mIn.readByte() & 0xFF;
				Object key = decode(t1);
				t1 = mIn.readByte() & 0xFF;
				Object value = decode(t1);
				result.put(key, value);
			}
			return result;
		}
		else {
			throw new IOException("Unknown data type " + t);
		}
	}
	
	private byte[] readWhile(byte c) throws IOException {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		byte b;
		while ((b = mIn.readByte()) != c) {
			s.write(b);
		}
		return s.toByteArray();
	}
	
	public boolean isUseUtf() {
		return mUseUtf;
	}

	public void setUseUtf(boolean useUtf) {
		mUseUtf = useUtf;
	}

	private DataInputStream mIn;
	private boolean mUseUtf;
}
