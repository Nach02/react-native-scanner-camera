import {useCallback, useEffect, useState} from 'react';
import {Platform} from 'react-native';
import {
  check,
  Permission,
  PERMISSIONS,
  request,
  RESULTS,
} from 'react-native-permissions';

const CAMERA_PERMISSION: Permission = Platform.select({
  ios: PERMISSIONS.IOS.CAMERA,
  android: PERMISSIONS.ANDROID.CAMERA,
})!;

export function useCameraPermission() {
  const [hasPermission, setHasPermission] = useState(false);

  useEffect(() => {
    check(CAMERA_PERMISSION).then(status => {
      if (status === RESULTS.GRANTED || status === RESULTS.LIMITED) {
        setHasPermission(true);
      } else {
        request(CAMERA_PERMISSION).then(result => {
          setHasPermission(
            result === RESULTS.GRANTED || result === RESULTS.LIMITED,
          );
        });
      }
    });
  }, []);

  const requestPermission = useCallback(async (): Promise<boolean> => {
    const result = await request(CAMERA_PERMISSION);
    const granted =
      result === RESULTS.GRANTED || result === RESULTS.LIMITED;
    setHasPermission(granted);
    return granted;
  }, []);

  return {hasPermission, requestPermission};
}
