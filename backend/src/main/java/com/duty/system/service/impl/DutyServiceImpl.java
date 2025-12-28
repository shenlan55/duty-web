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
                        
                        // 只有oncall组的用户需要检查是否需要休息（上24小时班后休息2天）
                        // oncall组的groupId是1或2
                        if (user.getGroupId() == 1 || user.getGroupId() == 2) {
                            Date lastDutyDate = userLast24hDutyDate.get(user.getId());
                            if (lastDutyDate != null) {
                                long diffInDays = (currentDate.getTime() - lastDutyDate.getTime()) / (1000 * 60 * 60 * 24);
                                // 如果距离上次24小时班不足3天，则需要休息
                                if (diffInDays < 3) {
                                    System.out.println("用户 " + user.getName() + " (ID: " + user.getId() + ") 在 " + 
                                            lastDutyDate + " 上了24小时班，需要休息到 " + 
                                            new Date(lastDutyDate.getTime() + 3 * 24 * 60 * 60 * 1000) + 
                                            "，因此在 " + currentDate + " 不可用");
                                    return false;
                                }
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            
            System.out.println("日期 " + currentDate + " 可用用户数量: " + dayAvailableUsers.size());
            
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
            
            // 1. 确定当前值班的oncall组（9天一个周期）
            boolean isAGroupOnDuty = (dayOfPlan / 9) % 2 == 0;
            
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
            
            // 3.1 处理oncall大组长排班
            // oncall在岗大组长和不在岗大组长
            User actualOnDutyLeader = onDutyLeader;
            User actualOffDutyLeader = offDutyLeader;
            User actualGocLeader = dayGocLeader;
            
            // 处理oncall在岗大组长请假的情况：不在岗的大组长顶岗
            if (actualOnDutyLeader == null) {
                actualOnDutyLeader = offDutyLeader;
            }
            
            // 处理不在oncall岗的大组长请假的情况：如果goc组大组长可用，则让goc组大组长兼岗
            if (actualOffDutyLeader == null && actualGocLeader != null) {
                actualOffDutyLeader = actualGocLeader;
            }
            
            // 处理goc组大组长请假的情况：让不在oncall岗的大组长兼岗
            if (actualGocLeader == null && actualOffDutyLeader != null) {
                actualGocLeader = actualOffDutyLeader;
            }
            
            // oncall-A大组长排班
            if (dayShift != null) {
                // oncall-A大组长
                if (isAGroupOnDuty) {
                    // oncall-A组在岗，安排实际在岗大组长
                    if (actualOnDutyLeader != null) {
                        DutyPlan oncallALeaderPlan = new DutyPlan();
                        oncallALeaderPlan.setUserId(actualOnDutyLeader.getId());
                        oncallALeaderPlan.setDate(currentDate);
                        oncallALeaderPlan.setTimeSlotId(dayShift.getId());
                        oncallALeaderPlan.setType(1); // 白班
                        oncallALeaderPlan.setStatus(1);
                        plans.add(oncallALeaderPlan);
                        
                        // 设置兼大组：如果实际在岗大组长不是oncall-A组的，设置兼大组
                        if (!actualOnDutyLeader.getGroupId().equals(oncallA.get(0).getGroupId())) {
                            oncallALeaderPlan.setDutyGroupId(oncallA.get(0).getGroupId());
                        }
                    }
                } else {
                    // oncall-A组不在岗，安排实际不在岗大组长
                    if (actualOffDutyLeader != null) {
                        DutyPlan oncallALeaderPlan = new DutyPlan();
                        oncallALeaderPlan.setUserId(actualOffDutyLeader.getId());
                        oncallALeaderPlan.setDate(currentDate);
                        oncallALeaderPlan.setTimeSlotId(dayShift.getId());
                        oncallALeaderPlan.setType(1); // 白班
                        oncallALeaderPlan.setStatus(1);
                        plans.add(oncallALeaderPlan);
                        
                        // 设置兼大组：如果实际不在岗大组长不是oncall-A组的，设置兼大组
                        if (!actualOffDutyLeader.getGroupId().equals(oncallA.get(0).getGroupId())) {
                            oncallALeaderPlan.setDutyGroupId(oncallA.get(0).getGroupId());
                        }
                    }
                }
                
                // oncall-B大组长
                if (!isAGroupOnDuty) {
                    // oncall-B组在岗，安排实际在岗大组长
                    if (actualOnDutyLeader != null) {
                        DutyPlan oncallBLeaderPlan = new DutyPlan();
                        oncallBLeaderPlan.setUserId(actualOnDutyLeader.getId());
                        oncallBLeaderPlan.setDate(currentDate);
                        oncallBLeaderPlan.setTimeSlotId(dayShift.getId());
                        oncallBLeaderPlan.setType(1); // 白班
                        oncallBLeaderPlan.setStatus(1);
                        plans.add(oncallBLeaderPlan);
                        
                        // 设置兼大组：如果实际在岗大组长不是oncall-B组的，设置兼大组
                        if (!actualOnDutyLeader.getGroupId().equals(oncallB.get(0).getGroupId())) {
                            oncallBLeaderPlan.setDutyGroupId(oncallB.get(0).getGroupId());
                        }
                    }
                } else {
                    // oncall-B组不在岗，安排实际不在岗大组长
                    if (actualOffDutyLeader != null) {
                        DutyPlan oncallBLeaderPlan = new DutyPlan();
                        oncallBLeaderPlan.setUserId(actualOffDutyLeader.getId());
                        oncallBLeaderPlan.setDate(currentDate);
                        oncallBLeaderPlan.setTimeSlotId(dayShift.getId());
                        oncallBLeaderPlan.setType(1); // 白班
                        oncallBLeaderPlan.setStatus(1);
                        plans.add(oncallBLeaderPlan);
                        
                        // 设置兼大组：如果实际不在岗大组长不是oncall-B组的，设置兼大组
                        if (!actualOffDutyLeader.getGroupId().equals(oncallB.get(0).getGroupId())) {
                            oncallBLeaderPlan.setDutyGroupId(oncallB.get(0).getGroupId());
                        }
                    }
                }
            }
            
            // 3.3 goc大组长排班
            if (dayShift != null && actualGocLeader != null) {
                DutyPlan gocLeaderPlan = new DutyPlan();
                gocLeaderPlan.setUserId(actualGocLeader.getId());
                gocLeaderPlan.setDate(currentDate);
                gocLeaderPlan.setTimeSlotId(dayShift.getId());
                gocLeaderPlan.setType(1); // 白班
                gocLeaderPlan.setStatus(1);
                plans.add(gocLeaderPlan);
                
                // 设置兼大组：如果实际goc大组长不是goc组的，设置兼大组
                if (!actualGocLeader.getGroupId().equals(gocGroup.get(0).getGroupId())) {
                    gocLeaderPlan.setDutyGroupId(gocGroup.get(0).getGroupId());
                }
            }
            

            
            // 4. 处理oncall组员排班 - 只有oncall在岗大组上24小时班 休 休
            // 4.1 准备备用人员池（用于补充oncall组）
            List<User> backupPool = new ArrayList<>();
            
            // 4.2 24小时班 休 休 排班：每3天一个周期，每个小组在周期内轮到一次
            // 每天都要有一组两人上24小时班，按照小组顺序轮流
            // 例如：小组1在第1天值班，小组2在第2天值班，小组3在第3天值班
            // 第4天回到小组1值班，以此类推
            
            // 记录当天上24小时班的oncall组员
            List<User> on24hDutyUsers = new ArrayList<>();
            
            // 计算当前oncall组内部的天数（0-2），用于正确轮动小组
            int onDutyGroupDay = dayOfPlan % 3;
            
            // 计算当前应该轮到哪个小组：每个oncall组内部每天换一个小组
            // 小组编号从1开始
            int teamIndex = onDutyGroupDay + 1;
            
            // 获取所有可用的oncall组员（排除大组长，且可以上班）
            List<User> availableOnDutyMembers = new ArrayList<>();
            for (User user : onDutyMembers) {
                boolean canWork = canUserWorkOnDate(user, currentDate);
                // 检查是否需要休息
                if (user.getGroupId() == 1 || user.getGroupId() == 2) {
                    Date lastDutyDate = userLast24hDutyDate.get(user.getId());
                    if (lastDutyDate != null) {
                        long diffInDays = (currentDate.getTime() - lastDutyDate.getTime()) / (1000 * 60 * 60 * 24);
                        if (diffInDays < 3) {
                            canWork = false;
                        }
                    }
                }
                if (canWork) {
                    availableOnDutyMembers.add(user);
                }
            }
            
            // 从可用的oncall组员中选择当前小组的人员
            // 按照小组编号选择对应的人员
            List<User> currentTeam = new ArrayList<>();
            // 查找小组编号为teamIndex的人员
            for (User user : availableOnDutyMembers) {
                if (user.getSubGroupId() == teamIndex) {
                    currentTeam.add(user);
                }
            }
            
            // 如果当前小组人数不足2人，从另一oncall组组员中补充
            List<User> tempBackupPool = new ArrayList<>(offDutyMembers);
            // 用于记录已经被使用的备份人员ID
            List<Long> usedBackupUserIds = new ArrayList<>();
            
            while (currentTeam.size() < 2 && !tempBackupPool.isEmpty()) {
                // 检查备份人员是否可以上班
                User backupUser = tempBackupPool.remove(0);
                boolean canWork = canUserWorkOnDate(backupUser, currentDate);
                if (backupUser.getGroupId() == 1 || backupUser.getGroupId() == 2) {
                    Date lastDutyDate = userLast24hDutyDate.get(backupUser.getId());
                    if (lastDutyDate != null) {
                        long diffInDays = (currentDate.getTime() - lastDutyDate.getTime()) / (1000 * 60 * 60 * 24);
                        if (diffInDays < 3) {
                            canWork = false;
                        }
                    }
                }
                if (canWork) {
                    currentTeam.add(backupUser);
                    usedBackupUserIds.add(backupUser.getId());
                }
            }
            
            // 从offDutyMembers中移除已经被使用的备份人员，避免重复分配
            offDutyMembers.removeIf(user -> usedBackupUserIds.contains(user.getId()));
            
            // 为当前小组安排24小时班
            for (User user : currentTeam) {
                DutyPlan plan = new DutyPlan();
                plan.setUserId(user.getId());
                plan.setDate(currentDate);
                plan.setTimeSlotId(timeSlot24h.getId());
                plan.setType(3); // 24小时班
                plan.setStatus(1);
                // 设置小组ID
                plan.setAssignedSubGroupId(teamIndex);
                
                // 设置兼大组和兼小组
                // 如果是从另一组补充过来的，记录被分配到的大组和小组
                if (offDutyMembers.contains(user)) {
                    plan.setDutyGroupId(onDutyOncallGroup.get(0).getGroupId()); // 分配到的大组
                    plan.setDutySubGroupId(teamIndex); // 分配到的小组
                }
                
                plans.add(plan);
                
                // 记录用户上24小时班的日期
                userLast24hDutyDate.put(user.getId(), currentDate);
                
                // 记录当天上24小时班的用户
                on24hDutyUsers.add(user);
            }
            
            // 4.4 准备用于补充到其他组的人员池
            // 只有不在oncall岗的oncall组组员才会被添加到backupPool中，用于给goc组补岗
            // oncall在岗组内其他小组组员不参与补岗
            
            // 直接使用不在岗oncall组的所有组员作为备份池
            backupPool = new ArrayList<>(offDutyMembers);
            
            // 确保backupPool中不包含当天上了24小时班的用户
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
            // 从备份池中获取剩余人员，按照oncall组的小组顺序补充
            List<User> remainingBackup = new ArrayList<>(backupPool);
            
            // 优先往单人小组（第4组）补充到3人
            if (gocTeams.get(3).size() < 3 && !remainingBackup.isEmpty()) {
                while (gocTeams.get(3).size() < 3 && !remainingBackup.isEmpty()) {
                    gocTeams.get(3).add(remainingBackup.remove(0));
                }
            }
            
            // 剩余人员补充到goc大组的其他小组，按照小组顺序1、2、3补充
            // 修复：循环直到remainingBackup为空或所有小组都满员为止
            int i = 0;
            while (!remainingBackup.isEmpty()) {
                int teamNum = i % 3; // 0-2对应goc小组1-3
                if (gocTeams.get(teamNum).size() < 3) {
                    gocTeams.get(teamNum).add(remainingBackup.remove(0));
                }
                i++;
                // 防止无限循环：如果连续检查了3次都没有添加人员，说明所有小组都满了
                if (i > 3 && gocTeams.get(0).size() >= 3 && gocTeams.get(1).size() >= 3 && gocTeams.get(2).size() >= 3) {
                    break;
                }
            }
            
            // 5.4 将goc组不需要的剩余人员放回backupPool，用于补充到pm组
            backupPool = remainingBackup;
            
            // 5.5 goc组员白班排班
            if (dayShift != null) {
                for (int j = 0; j < gocTeams.size(); j++) {
                    List<User> team = gocTeams.get(j);
                    for (User user : team) {
                        DutyPlan plan = new DutyPlan();
                        plan.setUserId(user.getId());
                        plan.setDate(currentDate);
                        plan.setTimeSlotId(dayShift.getId());
                        plan.setType(1); // 白班
                        plan.setStatus(1);
                        // 设置小组ID（1-4）
                        plan.setAssignedSubGroupId(j + 1);
                        
                        // 设置兼大组和兼小组
                        // 如果是从其他组补充过来的，记录被分配到的goc大组和小组
                        if (!gocGroup.contains(user)) {
                            plan.setDutyGroupId(gocGroup.get(0).getGroupId()); // 分配到的goc大组
                            plan.setDutySubGroupId(j + 1); // 分配到的goc小组
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
                
                // 2. 补充剩余的backupPool中的人员到pm组
                for (User user : backupPool) {
                    // 检查用户是否可以在当天上班
                    if (!canUserWorkOnDate(user, currentDate)) {
                        continue;
                    }
                    
                    // 检查是否已经被分配到其他组
                    boolean isAlreadyAssigned = false;
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
                            // 确保pmGroup不为空，避免NullPointerException
                            if (!pmGroup.isEmpty()) {
                                pmPlan.setDutyGroupId(pmGroup.get(0).getGroupId()); // 分配到的pm大组
                            }
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
        // 如果状态为正常，清空请假时间
        if (user.getStatus() != null && user.getStatus() == 1) {
            user.setLeaveStartDate(null);
            user.setLeaveEndDate(null);
        }
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
        // 如果没有设置请假时间，都可以排班
        return true;
    }
}