import React, {useRef} from 'react';
import {
  requireNativeComponent,
  StyleProp,
  ViewStyle,
  NativeSyntheticEvent,
} from 'react-native';

const NativeScanner = requireNativeComponent<{
  codeTypes: string[];
  scanAreaFraction?: number;
  onScan?: (event: NativeSyntheticEvent<{value: string}>) => void;
  style?: StyleProp<ViewStyle>;
}>('Scanner');

export interface ScannerProps {
  codeTypes: string[];
  /** Fraction of the center of the camera image to scan (0.0–1.0). Default: 1.0 (full frame). */
  scanAreaFraction?: number;
  onScan: (value: string) => void;
  style?: StyleProp<ViewStyle>;
}

const Scanner: React.FC<ScannerProps> = ({onScan, codeTypes, scanAreaFraction, style}) => {
  const onScanRef = useRef(onScan);
  onScanRef.current = onScan;

  const handleScan = (event: NativeSyntheticEvent<{value: string}>) => {
    onScanRef.current(event.nativeEvent.value);
  };

  return (
    <NativeScanner
      style={style}
      codeTypes={codeTypes}
      scanAreaFraction={scanAreaFraction}
      onScan={handleScan}
    />
  );
};

export {Scanner};
export {Camera} from './Camera';
export type {CameraRef, CameraProps} from './Camera';
export {useCameraPermission} from './useCameraPermission';
