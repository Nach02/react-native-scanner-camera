import React, {forwardRef, useImperativeHandle, useRef} from 'react';
import {
  NativeSyntheticEvent,
  Platform,
  requireNativeComponent,
  UIManager,
  StyleProp,
  ViewStyle,
  findNodeHandle,
} from 'react-native';

const NativeCamera = requireNativeComponent<{
  onReady?: (event: NativeSyntheticEvent<{}>) => void;
  onPhotoTaken?: (event: NativeSyntheticEvent<{uri: string; base64: string}>) => void;
  onPhotoError?: (event: NativeSyntheticEvent<{message: string}>) => void;
  style?: StyleProp<ViewStyle>;
}>('Camera');

export interface CameraRef {
  takePhoto(): Promise<{uri: string; base64: string}>;
}

export interface CameraProps {
  onReady?: () => void;
  style?: StyleProp<ViewStyle>;
}

export const Camera = forwardRef<CameraRef, CameraProps>(
  ({onReady, style}, ref) => {
    const nativeRef = useRef<any>(null);
    const resolveRef = useRef<((v: {uri: string; base64: string}) => void) | null>(null);
    const rejectRef = useRef<((e: Error) => void) | null>(null);

    useImperativeHandle(ref, () => ({
      takePhoto: () =>
        new Promise<{uri: string; base64: string}>((resolve, reject) => {
          const tag = findNodeHandle(nativeRef.current);
          if (!tag) {
            reject(new Error('Camera not ready'));
            return;
          }
          resolveRef.current = resolve;
          rejectRef.current = reject;
          const commandId =
            Platform.OS === 'android'
              ? UIManager.getViewManagerConfig('Camera').Commands.takePhoto
              : 'takePhoto';
          UIManager.dispatchViewManagerCommand(tag, commandId, []);
        }),
    }));

    return (
      <NativeCamera
        ref={nativeRef}
        style={style}
        onReady={() => onReady?.()}
        onPhotoTaken={e => {
          resolveRef.current?.(e.nativeEvent);
          resolveRef.current = null;
          rejectRef.current = null;
        }}
        onPhotoError={e => {
          rejectRef.current?.(new Error(e.nativeEvent.message));
          resolveRef.current = null;
          rejectRef.current = null;
        }}
      />
    );
  },
);
