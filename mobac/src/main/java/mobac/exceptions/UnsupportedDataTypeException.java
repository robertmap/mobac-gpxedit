package mobac.exceptions;

import java.io.IOException;

public class UnsupportedDataTypeException extends IOException {

	public UnsupportedDataTypeException(String dataType) {
		super("Unsupported data type: " + dataType);
	}
}
