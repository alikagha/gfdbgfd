package li.vin.bt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kyle on 6/13/14.
 */
public final class Device implements Parcelable {
  private final BluetoothDevice mDevice;

  /*package*/ Device(BluetoothDevice device) {
    mDevice = device;
  }

  private Device(Parcel in) {
    mDevice = in.readParcelable(null);
  }

  public String getName() {
    String name = mDevice.getName();
    if (name == null) {
      name = mDevice.getAddress();
    }
    return name;
  }

  public DeviceInterface createDeviceInterface(Context context) {
    return new BtLeDeviceInterface(context, mDevice);
  }

  @Override public void writeToParcel(Parcel out, int flags) {
    out.writeParcelable(mDevice, flags);
  }

  @Override public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
    public Device createFromParcel(Parcel in) {
      return new Device(in);
    }

    public Device[] newArray(int size) {
      return new Device[size];
    }
  };
}
