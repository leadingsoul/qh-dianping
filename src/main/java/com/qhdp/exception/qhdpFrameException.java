package com.qhdp.exception;


import lombok.Data;
import lombok.EqualsAndHashCode;
import com.qhdp.enums.BaseCode;

/**
 * &#064;description:  业务异常
 * &#064;author: phoenix
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class qhdpFrameException extends BaseException {
	
	private Integer code;
	
	private String message;

	public qhdpFrameException() {
		super();
	}

	public qhdpFrameException(String message) {
		super(message);
	}
	
	public qhdpFrameException(Integer code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}
	
	public qhdpFrameException(BaseCode baseCode) {
		super(baseCode.getMsg());
		this.code = baseCode.getCode();
		this.message = baseCode.getMsg();
	}

	public qhdpFrameException(Throwable cause) {
		super(cause);
	}

	public qhdpFrameException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}
}
