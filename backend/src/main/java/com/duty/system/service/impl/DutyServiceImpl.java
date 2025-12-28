package com.duty.system.service.impl;

import com.duty.system.entity.Config;
import com.duty.system.entity.DutyPlan;
import com.duty.system.entity.TimeSlot;
import com.duty.system.entity.User;
import com.duty.system.repository.ConfigRepository;
import com.duty.system.repository.DutyPlanRepository;
import com.duty.system.repository.TimeSlotRepository;
import com.duty.system.repository.UserRepository;
import com.duty.system.service.DutyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DutyServiceImpl implements DutyService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    @Autowired
    private DutyPlanRepository dutyPlanRepository;
    
    @Autowired
    private ConfigRepository configRepository;
    
    @Override
    public List<DutyPlan> generateDutyPlan(Date startDate, Date endDate) {
        // 生成新排班计划前，先删除该日期范围内的现有计划，避免重复
        deleteDutyPlans(startDate, endDate);
        
        List<DutyPlan> plans = new ArrayList<>();
        
        // 获取所有时间段
        List<TimeSlot> timeSlots = timeSlotRepository.findByStatus(1);
        
        // 获取各类型时间段
        TimeSlot dayShift = timeSlots.stream().filter(ts -> ts.getType() == 1).findFirst().orElse(null);
        TimeSlot timeSlot24h = timeSlots.stream().filter(ts -> ts.getType() == 3).findFirst().orElse(null);
        
        // 如果没有24小时时间段，创建一个默认的
        if (timeSlot24h == null) {
            timeSlot24h = new TimeSlot();
            timeSlot24h.setType(3);
            timeSlot24h.setName("24小时班");
            timeSlot24h.setStartTime("08:00");
            timeSlot24h.setEndTime("08:00");
            timeSlot24h.setStatus(1);
            timeSlot24h = timeSlotRepository.save(timeSlot24h);
            timeSlots.add(timeSlot24h);
        }
        
        // 获取所有用户，不在这里过滤，而是在每天排班时通过canUserWorkOnDate方法检查
        List<User> allUsers = userRepository.findAll();
        
        // 按大组分组
        List<User> oncallA = allUsers.stream().filter(user -> user.getGroupId() == 1).collect(Collectors.toList());
        List<User> oncallB = allUsers.stream().filter(user -> user.getGroupId() == 2).collect(Collectors.toList());
        List<User> gocGroup = allUsers.stream().filter(user -> user.getGroupId() == 3).collect(Collectors.toList());
        List<User> pmGroup = allUsers.stream().filter(user -> user.getGroupId() == 4).collect(Collectors.toList());
        
        // 获取各组长
        User oncallALeader = oncallA.stream().filter(User::getIsGroupLeader).findFirst().orElse(null);
        User oncallBLeader = oncallB.stream().filter(User::getIsGroupLeader).findFirst().orElse(null);
        User gocLeader = gocGroup.stream().filter(User::getIsGroupLeader).findFirst().orElse(null);
        
        // 提取oncall组员（排除大组长）
        List<User> oncallAMembers = oncallA.stream().filter(user -> !user.getIsGroupLeader()).collect(Collectors.toList());
        List<User> oncallBMembers = oncallB.stream().filter(user -> !user.getIsGroupLeader()).collect(Collectors.toList());
        
        // 提取goc组员（排除大组长）
        List<User> gocMembers = gocGroup.stream().filter(user -> !user.getIsGroupLeader()).collect(Collectors.toList());
        
        // 生成每天的排班
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        
        // 记录用户上24小时班的日期，用于判断休息时间
        java.util.Map<Long, Date> userLast24hDutyDate = new java.util.HashMap<>();
        
        while (!calendar.getTime().after(endDate)) {
            Date currentDate = calendar.getTime();
            
            // 计算当前是第几天
            int dayOfPlan = (int) ((calendar.getTimeInMillis() - startDate.getTime()) / (1000 * 60 * 60 * 24));
            
            // 为当天创建可用用户列表，过滤掉当天请假的用户和需要休息的用户
            List<User> dayAvailableUsers = allUsers.stream()
                    .filter(user -> {
                        // 首先检查是否当天可以上班
                        if (!canUserWorkOnDate(user, currentDate)) {
                            return false;
                        }
                        
                        // 检查是否需要休息（上24小时班后休息2天）
                        Date lastDutyDate = userLast24hDutyDate.get(user.getId());
                        if (lastDutyDate != null) {
                            long diffInDays = (currentDate.getTime() - lastDutyDate.getTime()) / (1000 * 60 * 60 * 24);
                            // 如果距离上次24小时班不足3天，则需要休息
                            if (diffInDays < 3) {
                                return false;
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            
            // 当天可用用户按大组分组，注意dayAvailableUsers已经过滤了需要休息的用户
            List<User> dayOncallA = dayAvailableUsers.stream().filter(user -> user.getGroupId() == 1).collect(Collectors.toList());
            List<User> dayOncallB = dayAvailableUsers.stream().filter(user -> user.getGroupId() == 2).collect(Collectors.toList());
            List<User> dayGocGroup = dayAvailableUsers.stream().filter(user -> user.getGroupId() == 3).collect(Collectors.toList());
            List<User> dayPmGroup = dayAvailableUsers.stream().filter(user -> user.getGroupId() == 4).collect(Collectors.toList());
            
            // 当天可用的各组长
            User dayOncallALeader = dayOncallA.stream().filter(User::getIsGroupLeader).findFirst().orElse(null);
            User dayOncallBLeader = dayOncallB.stream().filter(User::getIsGroupLeader).findFirst().orElse(null);
            User dayGocLeader = dayGocGroup.stream().filter(User::getIsGroupLeader).findFirst().orElse(null);
            
            // 当天可用的oncall组员（排除大组长）
            List<User> dayOncallAMembers = dayOncallA.stream().filter(user -> !user.getIsGroupLeader()).collect(Collectors.toList());
            List<User> dayOncallBMembers = dayOncallB.stream().filter(user -> !user.getIsGroupLeader()).collect(Collectors.toList());
            
            // 当天可用的goc组员（排除大组长）
            List<User> dayGocMembers = dayGocGroup.stream().filter(user -> !user.getIsGroupLeader()).collect(Collectors.toList());
            
            // 1. 确定当前值班的oncall组（3天一个周期）
            boolean isAGroupOnDuty = (dayOfPlan / 3) % 2 == 0;
            
            // 2. 准备值班人员
            // 2.1 oncall在岗组和不在岗组
            List<User> onDutyOncallGroup = isAGroupOnDuty ? dayOncallA : dayOncallB;
            List<User> offDutyOncallGroup = isAGroupOnDuty ? dayOncallB : dayOncallA;
            
            // 2.2 oncall在岗大组长和不在岗大组长
            User onDutyLeader = isAGroupOnDuty ? dayOncallALeader : dayOncallBLeader;
            User offDutyLeader = isAGroupOnDuty ? dayOncallBLeader : dayOncallALeader;
            
            // 2.3 oncall在岗组员和不在岗组员
            List<User> onDutyMembers = isAGroupOnDuty ? dayOncallAMembers : dayOncallBMembers;
            List<User> offDutyMembers = isAGroupOnDuty ? dayOncallBMembers : dayOncallAMembers;
            
            // 3. 处理大组长排班
            
            // 3.1 oncall-A大组长
            if (dayShift != null && dayOncallALeader != null) {
                DutyPlan oncallALeaderPlan = new DutyPlan();
                oncallALeaderPlan.setUserId(dayOncallALeader.getId());
                oncallALeaderPlan.setDate(currentDate);
                oncallALeaderPlan.setTimeSlotId(dayShift.getId());
                oncallALeaderPlan.setType(1); // 白班
                oncallALeaderPlan.setStatus(1);
                plans.add(oncallALeaderPlan);
            }
            
            // 3.2 oncall-B大组长
            if (dayShift != null && dayOncallBLeader != null) {
                DutyPlan oncallBLeaderPlan = new DutyPlan();
                oncallBLeaderPlan.setUserId(dayOncallBLeader.getId());
                oncallBLeaderPlan.setDate(currentDate);
                oncallBLeaderPlan.setTimeSlotId(dayShift.getId());
                oncallBLeaderPlan.setType(1); // 白班
                oncallBLeaderPlan.setStatus(1);
                plans.add(oncallBLeaderPlan);
            }
            
            // 3.3 goc大组长
            if (dayShift != null && dayGocLeader != null) {
                DutyPlan gocLeaderPlan = new DutyPlan();
                gocLeaderPlan.setUserId(dayGocLeader.getId());
                gocLeaderPlan.setDate(currentDate);
                gocLeaderPlan.setTimeSlotId(dayShift.getId());
                gocLeaderPlan.setType(1); // 白班
                gocLeaderPlan.setStatus(1);
                plans.add(gocLeaderPlan);
            }
            

            
            // 4. 处理oncall组员排班 - 只有oncall在岗大组上24小时班 休 休
            // 4.1 将oncall组员分成固定小组（每组2人）
            List<List<User>> onDutyTeams = new ArrayList<>();
            List<User> tempOnDutyMembers = new ArrayList<>(onDutyMembers);
            
            // 按2人一组分组
            for (int i = 0; i < tempOnDutyMembers.size(); i += 2) {
                if (i + 1 < tempOnDutyMembers.size()) {
                    onDutyTeams.add(Arrays.asList(tempOnDutyMembers.get(i), tempOnDutyMembers.get(i + 1)));
                }
            }
            
            // 4.2 准备备用人员池（用于补充oncall组）
            List<User> backupPool = new ArrayList<>(offDutyMembers);
            
            // 4.3 24小时班 休 休 排班：每3天一个周期，每个小组在周期内轮到一次
            // 每天都要有一组两人上24小时班，按照小组顺序轮流
            // 例如：小组1在第1天值班，小组2在第2天值班，小组3在第3天值班
            // 第4天回到小组1值班，以此类推
            
            // 记录当天上24小时班的oncall组员
            List<User> on24hDutyUsers = new ArrayList<>();
            
            // 确保每天都有小组值班
            if (!onDutyTeams.isEmpty()) {
                // 计算当前应该轮到哪个小组：每天换一个小组
                int teamIndex = dayOfPlan % onDutyTeams.size();
                List<User> currentTeam = onDutyTeams.get(teamIndex);
                
                // 检查当前小组是否有请假或需要休息的人员，如果有，从备份池中补充
                List<User> availableTeam = new ArrayList<>();
                List<User> tempBackupPool = new ArrayList<>(backupPool);
                
                for (User user : currentTeam) {
                    // 检查用户是否可以在当天排班
                    boolean canWork = canUserWorkOnDate(user, currentDate);
                    // 检查是否需要休息（上24小时班后休息2天）
                    Date lastDutyDate = userLast24hDutyDate.get(user.getId());
                    if (lastDutyDate != null) {
                        long diffInDays = (currentDate.getTime() - lastDutyDate.getTime()) / (1000 * 60 * 60 * 24);
                        // 如果距离上次24小时班不足3天，则需要休息
                        if (diffInDays < 3) {
                            canWork = false;
                        }
                    }
                    
                    if (canWork) {
                        availableTeam.add(user);
                        on24hDutyUsers.add(user);
                    } else if (!tempBackupPool.isEmpty()) {
                        // 从备份池中补充，补充人员已经经过休息检查
                        User replacement = tempBackupPool.remove(0);
                        availableTeam.add(replacement);
                        on24hDutyUsers.add(replacement);
                    }
                }
                
                // 为当前小组安排24小时班
                for (User user : availableTeam) {
                    DutyPlan plan = new DutyPlan();
                    plan.setUserId(user.getId());
                    plan.setDate(currentDate);
                    plan.setTimeSlotId(timeSlot24h.getId());
                    plan.setType(3); // 24小时班
                    plan.setStatus(1);
                    // 设置小组ID
                    plan.setAssignedSubGroupId(teamIndex + 1);
                    
                    // 设置兼大组和兼小组
                    // 如果是从另一组补充过来的，记录被分配到的大组和小组
                    if (offDutyMembers.contains(user)) {
                        plan.setDutyGroupId(onDutyOncallGroup.get(0).getGroupId()); // 分配到的大组
                        plan.setDutySubGroupId(teamIndex + 1); // 分配到的小组
                    }
                    
                    plans.add(plan);
                    
                    // 记录用户上24小时班的日期
                    userLast24hDutyDate.put(user.getId(), currentDate);
                }
            } else if (!backupPool.isEmpty()) {
                // 如果没有可用小组，从备份池中抽取2人组成临时小组
                List<User> tempTeam = new ArrayList<>();
                tempTeam.add(backupPool.remove(0));
                if (!backupPool.isEmpty()) {
                    tempTeam.add(backupPool.remove(0));
                }
                
                // 为临时小组安排24小时班
                for (User user : tempTeam) {
                    DutyPlan plan = new DutyPlan();
                    plan.setUserId(user.getId());
                    plan.setDate(currentDate);
                    plan.setTimeSlotId(timeSlot24h.getId());
                    plan.setType(3); // 24小时班
                    plan.setStatus(1);
                    plan.setAssignedSubGroupId(1); // 默认小组1
                    
                    // 设置兼大组和兼小组
                    plan.setDutyGroupId(onDutyOncallGroup.get(0).getGroupId()); // 分配到的大组
                    plan.setDutySubGroupId(1); // 分配到的小组
                    
                    plans.add(plan);
                    
                    // 记录用户上24小时班的日期
                    userLast24hDutyDate.put(user.getId(), currentDate);
                }
            }
            
            // 4.4 将oncall在岗组中没有上24小时班的组员添加到backupPool，用于补充到其他组
            for (User user : onDutyMembers) {
                // 检查用户是否当天上了24小时班
                boolean isOn24hDuty = false;
                for (User on24hUser : on24hDutyUsers) {
                    if (on24hUser.getId().equals(user.getId())) {
                        isOn24hDuty = true;
                        break;
                    }
                }
                if (!isOn24hDuty) {
                    backupPool.add(user);
                }
            }
            
            // 4.5 确保backupPool中不包含当天上了24小时班的用户
            List<User> filteredBackupPool = new ArrayList<>();
            for (User user : backupPool) {
                boolean isOn24hDuty = false;
                for (User on24hUser : on24hDutyUsers) {
                    if (on24hUser.getId().equals(user.getId())) {
                        isOn24hDuty = true;
                        break;
                    }
                }
                if (!isOn24hDuty) {
                    filteredBackupPool.add(user);
                }
            }
            backupPool = filteredBackupPool;
            
            // 5. 处理goc组员排班 - 所有goc组人员都上白班
            // 5.1 准备goc小组
            List<List<User>> gocTeams = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                gocTeams.add(new ArrayList<>());
            }
            
            // 5.2 分配goc组员到小组 - 使用当天可用的goc组员
            List<User> tempGocMembers = new ArrayList<>(dayGocMembers);
            
            // 前3组每组2人
            for (int i = 0; i < 3 && !tempGocMembers.isEmpty(); i++) {
                gocTeams.get(i).add(tempGocMembers.remove(0));
                if (!tempGocMembers.isEmpty()) {
                    gocTeams.get(i).add(tempGocMembers.remove(0));
                }
            }
            
            // 第4组1人常驻
            if (!tempGocMembers.isEmpty()) {
                gocTeams.get(3).add(tempGocMembers.remove(0));
            }
            
            // 5.3 补充不在岗的oncall组员到goc小组 - 补充到goc组的上白班
            // 从备份池中获取剩余人员
            List<User> remainingBackup = new ArrayList<>(backupPool);
            
            // 优先往单人小组补充到3人
            if (gocTeams.get(3).size() < 3 && !remainingBackup.isEmpty()) {
                while (gocTeams.get(3).size() < 3 && !remainingBackup.isEmpty()) {
                    gocTeams.get(3).add(remainingBackup.remove(0));
                }
            }
            
            // 剩余人员补充到其他小组
            for (int i = 0; i < 3 && !remainingBackup.isEmpty(); i++) {
                gocTeams.get(i).add(remainingBackup.remove(0));
            }
            
            // 5.4 goc组员白班排班
            if (dayShift != null) {
                for (int i = 0; i < gocTeams.size(); i++) {
                    List<User> team = gocTeams.get(i);
                    for (User user : team) {
                        DutyPlan plan = new DutyPlan();
                        plan.setUserId(user.getId());
                        plan.setDate(currentDate);
                        plan.setTimeSlotId(dayShift.getId());
                        plan.setType(1); // 白班
                        plan.setStatus(1);
                        // 设置小组ID（1-4）
                        plan.setAssignedSubGroupId(i + 1);
                        
                        // 设置兼大组和兼小组
                        // 如果是从oncall组补充过来的，记录被分配到的goc大组
                        if (offDutyMembers.contains(user) || onDutyMembers.contains(user)) {
                            plan.setDutyGroupId(gocGroup.get(0).getGroupId()); // 分配到的goc大组
                            plan.setDutySubGroupId(i + 1); // 分配到的goc小组
                        }
                        
                        plans.add(plan);
                    }
                }
            }
            
            // 6. 处理pm组排班（每天上白班）
            if (dayShift != null) {
                // 1. 先安排当天可用的pm组人员
                for (User pmUser : dayPmGroup) {
                    DutyPlan pmPlan = new DutyPlan();
                    pmPlan.setUserId(pmUser.getId());
                    pmPlan.setDate(currentDate);
                    pmPlan.setTimeSlotId(dayShift.getId());
                    pmPlan.setType(1); // 白班
                    // 不设置小组ID，项目经理和大组长不要挂小组
                    pmPlan.setStatus(1);
                    
                    plans.add(pmPlan);
                }
                
                // 2. 准备用于补充到pm组的候选人员池
                List<User> pmBackupPool = new ArrayList<>();
                
                // 2.1 添加backupPool中的人员（不在岗的oncall组员和oncall在岗组中没有上24小时班的组员）
                pmBackupPool.addAll(backupPool);
                
                // 2.2 添加tempGocMembers中未分配的人员
                pmBackupPool.addAll(tempGocMembers);
                
                // 2.3 去重，确保同一人员不会被多次添加
                List<User> uniquePmBackup = new ArrayList<>();
                for (User user : pmBackupPool) {
                    if (!uniquePmBackup.contains(user)) {
                        uniquePmBackup.add(user);
                    }
                }
                
                // 2.4 筛选出未被安排到其他组的人员
                for (User user : uniquePmBackup) {
                    boolean isAlreadyAssigned = false;
                    // 检查该用户是否已经被安排到其他组
                    for (DutyPlan plan : plans) {
                        if (plan.getUserId().equals(user.getId())) {
                            isAlreadyAssigned = true;
                            break;
                        }
                    }
                    if (!isAlreadyAssigned) {
                        // 为该人员创建pm组排班计划
                        DutyPlan pmPlan = new DutyPlan();
                        pmPlan.setUserId(user.getId());
                        pmPlan.setDate(currentDate);
                        pmPlan.setTimeSlotId(dayShift.getId());
                        pmPlan.setType(1); // 白班
                        // 不设置小组ID，项目经理和大组长不要挂小组
                        pmPlan.setStatus(1);
                        
                        // 设置兼大组和兼小组
                        // 如果是从其他组补充过来的，记录被分配到的pm大组，兼小组为无
                        if (!pmGroup.contains(user)) {
                            pmPlan.setDutyGroupId(pmGroup.get(0).getGroupId()); // 分配到的pm大组
                            // 兼小组设为null，前端显示为"无"
                        }
                        
                        plans.add(pmPlan);
                    }
                }
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return dutyPlanRepository.saveAll(plans);
    }
    
    @Override
    public void deleteDutyPlans(Date startDate, Date endDate) {
        // 将endDate的时间设置为23:59:59，确保包含endDate当天的所有记录
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date adjustedEndDate = calendar.getTime();
        
        List<DutyPlan> plans = dutyPlanRepository.findByDateBetween(startDate, adjustedEndDate);
        if (!plans.isEmpty()) {
            dutyPlanRepository.deleteAll(plans);
        }
    }
    
    @Override
    public void deleteDutyPlansByIds(List<Long> ids) {
        dutyPlanRepository.deleteAllById(ids);
    }
    
    @Override
    public List<TimeSlot> getTimeSlots() {
        return timeSlotRepository.findAll();
    }
    
    @Override
    public TimeSlot saveTimeSlot(TimeSlot timeSlot) {
        return timeSlotRepository.save(timeSlot);
    }
    
    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    @Override
    public List<DutyPlan> getDutyPlans(Date startDate, Date endDate) {
        // 将endDate的时间设置为23:59:59，确保包含endDate当天的所有记录
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date adjustedEndDate = calendar.getTime();
        
        return dutyPlanRepository.findByDateBetween(startDate, adjustedEndDate);
    }
    
    @Override
    public List<Config> getConfigs() {
        return configRepository.findAll();
    }
    
    @Override
    public Config saveConfig(Config config) {
        return configRepository.save(config);
    }
    
    @Override
    public String getConfigValueByKey(String configKey) {
        return configRepository.findByConfigKey(configKey)
                .map(Config::getConfigValue)
                .orElse(null);
    }
    
    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    /**
     * 检查用户在特定日期是否可以排班（即不在请假时间范围内）
     * @param user 用户
     * @param date 排班日期
     * @return 是否可以排班
     */
    private boolean canUserWorkOnDate(User user, Date date) {
        // 检查用户是否在请假时间范围内
        Date leaveStart = user.getLeaveStartDate();
        Date leaveEnd = user.getLeaveEndDate();
        if (leaveStart != null && leaveEnd != null) {
            // 设置date的时间为中午，避免时区问题
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date checkDate = calendar.getTime();
            
            // 同样处理请假时间的时间部分
            Calendar leaveStartCal = Calendar.getInstance();
            leaveStartCal.setTime(leaveStart);
            leaveStartCal.set(Calendar.HOUR_OF_DAY, 0);
            leaveStartCal.set(Calendar.MINUTE, 0);
            leaveStartCal.set(Calendar.SECOND, 0);
            Date adjustedLeaveStart = leaveStartCal.getTime();
            
            Calendar leaveEndCal = Calendar.getInstance();
            leaveEndCal.setTime(leaveEnd);
            leaveEndCal.set(Calendar.HOUR_OF_DAY, 23);
            leaveEndCal.set(Calendar.MINUTE, 59);
            leaveEndCal.set(Calendar.SECOND, 59);
            Date adjustedLeaveEnd = leaveEndCal.getTime();
            
            // 检查checkDate是否在请假时间范围内
            return !(checkDate.after(adjustedLeaveStart) && checkDate.before(adjustedLeaveEnd)) && 
                   !checkDate.equals(adjustedLeaveStart) && !checkDate.equals(adjustedLeaveEnd);
        }
        // 如果没有设置请假时间，或者status不是请假，都可以排班
        return true;
    }
}