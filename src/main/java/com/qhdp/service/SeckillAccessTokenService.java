package com.qhdp.service;

/**
 * &#064;description:  令牌
 * &#064;author:  phoenix
 **/
public interface SeckillAccessTokenService {
  
    boolean isEnabled();
 
    String issueAccessToken(Long voucherId, Long userId);
    
    boolean validateAndConsume(Long voucherId, Long userId, String token);
}