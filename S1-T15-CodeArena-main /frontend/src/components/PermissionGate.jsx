import React from 'react';

const evaluateCheck = (permissions, check) => {
    if (!permissions) {
        return false;
    }
    if (!check) {
        return true;
    }
    if (typeof check === 'function') {
        return Boolean(check(permissions));
    }
    if (Array.isArray(check)) {
        return check.every((flag) => Boolean(permissions?.[flag]));
    }
    return Boolean(permissions?.[check]);
};

const PermissionGate = ({ permissions, check, loading = false, fallback = null, children }) => {
    if (loading) {
        return null;
    }
    const allowed = evaluateCheck(permissions, check);
    if (!allowed) {
        return typeof fallback === 'function' ? fallback(permissions) : fallback;
    }
    return typeof children === 'function' ? children(permissions) : <>{children}</>;
};

export default PermissionGate;

