package com.qhdp.exception;

import lombok.Data;

/**
 * &#064;description:  参数错误
 * &#064;author: phoenix
 **/
@Data
public class ArgumentError {
	
	private String argumentName;
	
	private String message;
}
