package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.RollbackFailureLog;
import com.qhdp.service.RollbackFailureLogService;
import com.qhdp.mapper.RollbackFailureLogMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_rollback_failure_log(Redis回滚失败日志表)】的数据库操作Service实现
* @createDate 2026-03-11 14:31:41
*/
@Service
public class RollbackFailureLogServiceImpl extends ServiceImpl<RollbackFailureLogMapper, RollbackFailureLog>
    implements RollbackFailureLogService{

}




