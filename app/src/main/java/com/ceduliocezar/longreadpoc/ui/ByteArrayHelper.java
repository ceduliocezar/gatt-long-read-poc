package com.ceduliocezar.longreadpoc.ui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;



public class ByteArrayHelper {
    private static final int DEFAULT_MTU = 23;

    /**
     * Chop only bytes at the beginning of the array based on offset, if array is bigger than MTU
     * size Android OS will negotiate with central and it will issue multiple read requests.
     * <p>
     * Important note: When {@link BluetoothDevice#TRANSPORT_BREDR} is used by the central,
     * only a single read request is issued by the central regardless of MTU size.
     */
    public static byte[] skipBytes(final byte[] value, final int offset) {
        byte[] choppedValue = new byte[0];

        if (offset <= value.length) {
            choppedValue = Arrays.copyOfRange(value, offset, value.length);
        }
        return choppedValue;
    }

    public static byte[] chopBeginAndEnd(BluetoothGattCharacteristic characteristic, int offset) {

        // this is how it works currently with blessed lib
        // If data is longer than MTU - 1, cut the array. Only ATT_MTU - 1 bytes can be sent in Long Read.
        return copyOf(nonnullOf(characteristic.getValue()), offset, DEFAULT_MTU - 1);
    }

    private @NotNull
    static byte[] copyOf(@NotNull final byte[] source, final int offset, final int maxSize) {
        if (source.length > maxSize) {
            final int chunkSize = Math.min(source.length - offset, maxSize);
            return Arrays.copyOfRange(source, offset, offset + chunkSize);
        }
        return Arrays.copyOf(source, source.length);
    }

    @NotNull
    private static byte[] nonnullOf(@Nullable final byte[] source) {
        return (source == null) ? new byte[0] : source;
    }


}
