import { useEffect, useState } from 'react';
import { api } from '../context/AuthContext';

export const useStudyPermissions = (studyId) => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!studyId) {
            return;
        }

        let isMounted = true;
        setLoading(true);
        setError(null);

        api.get(`/api/studies/${studyId}/permissions`)
            .then((response) => {
                if (isMounted) {
                    setData(response.data);
                }
            })
            .catch((err) => {
                if (isMounted) {
                    setError(err.response?.data?.message || 'Permissions could not be loaded.');
                }
            })
            .finally(() => {
                if (isMounted) {
                    setLoading(false);
                }
            });

        return () => {
            isMounted = false;
        };
    }, [studyId]);

    return { data, loading, error };
};


