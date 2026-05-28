// 图表权限控制工具

// 检查用户是否有权限访问图表
export function checkChartPermission(chartType) {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userRole = getUserRole(user);

  switch (chartType) {
    case 'status':
      return checkStatusChartPermission(userRole);
    case 'department':
      return checkDepartmentChartPermission(userRole);
    default:
      return false;
  }
}

// 检查用户是否有修改权限
export function checkModifyPermission() {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userRole = getUserRole(user);
  
  // 只有系统管理员有修改权限
  return userRole === 'admin';
}

// 获取用户角色
function getUserRole(user) {
  if (!user) return 'user';

  // 直接检查用户名是否为admin
  if (user.username && user.username.toLowerCase() === 'admin') {
    return 'admin';
  }

  // 优先使用后端返回的角色
  if (user.roles && user.roles.length > 0) {
    // 检查是否有admin角色
    for (const role of user.roles) {
      let roleName = '';
      try {
        roleName = typeof role === 'string' ? role : role.name || role.code;
        roleName = roleName.toLowerCase();
        if (roleName.startsWith('role_')) {
          roleName = roleName.substring(5);
        }
        if (roleName === 'admin') {
          return 'admin';
        }
      } catch (e) {
        // 忽略错误
      }
    }

    // 检查是否有leader角色
    for (const role of user.roles) {
      let roleName = '';
      try {
        roleName = typeof role === 'string' ? role : role.name || role.code;
        roleName = roleName.toLowerCase();
        if (roleName.startsWith('role_')) {
          roleName = roleName.substring(5);
        }
        if (roleName === 'leader') {
          return 'leader';
        }
      } catch (e) {
        // 忽略错误
      }
    }

    // 检查是否有manager角色
    for (const role of user.roles) {
      let roleName = '';
      try {
        roleName = typeof role === 'string' ? role : role.name || role.code;
        roleName = roleName.toLowerCase();
        if (roleName.startsWith('role_')) {
          roleName = roleName.substring(5);
        }
        if (roleName === 'manager') {
          return 'manager';
        }
      } catch (e) {
        // 忽略错误
      }
    }
  }

  // 如果没有角色信息，通过用户名推断
  if (user.username) {
    const usernameLower = user.username.toLowerCase();
    if (usernameLower === 'admin') {
      return 'admin';
    } else if (usernameLower.startsWith('leader')) {
      return 'leader';
    } else if (usernameLower.startsWith('admin_')) {
      return 'manager';
    } else if (usernameLower.startsWith('user')) {
      return 'user';
    }
  }

  return 'user';
}

// 检查资产状态分布图表权限
function checkStatusChartPermission(userRole) {
  const rolePermissions = {
    'admin': true,
    'leader': true,
    'manager': true,
    'user': false
  };

  return rolePermissions[userRole] || false;
}

// 检查部门资产统计图表权限
function checkDepartmentChartPermission(userRole) {
  const rolePermissions = {
    'admin': true,
    'leader': true,
    'manager': true,
    'user': false
  };

  return rolePermissions[userRole] || false;
}

// 获取用户访问级别
export function getAccessLevel(chartType) {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userRole = getUserRole(user);

  switch (userRole) {
    case 'admin':
      return 4; // 完全访问
    case 'leader':
      return 2; // 只读访问
    case 'manager':
      return 3; // 部分数据访问
    case 'user':
      return 1; // 无访问权限
    default:
      return 1;
  }
}

// 检查是否有权限访问特定部门的数据
export function hasDepartmentAccess(departmentId) {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userRole = getUserRole(user);

  if (userRole === 'admin' || userRole === 'leader') {
    return true;
  }

  if (userRole === 'manager' && user.departmentId) {
    return user.departmentId === departmentId;
  }

  return false;
}
