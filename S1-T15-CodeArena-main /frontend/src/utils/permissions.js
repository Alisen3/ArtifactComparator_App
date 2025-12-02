export const hasPermission = (permissions, flag) => Boolean(permissions && permissions[flag]);

export const hasAllPermissions = (permissions, flags = []) =>
    flags.every((flag) => hasPermission(permissions, flag));

export const hasAnyPermission = (permissions, flags = []) =>
    flags.some((flag) => hasPermission(permissions, flag));

