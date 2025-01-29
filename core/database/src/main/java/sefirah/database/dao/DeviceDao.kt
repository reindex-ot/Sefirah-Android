package sefirah.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import sefirah.database.model.LocalDeviceEntity
import sefirah.database.model.RemoteDeviceEntity
import sefirah.database.model.DeviceNetworkCrossRef
import sefirah.database.model.NetworkEntity
import sefirah.database.model.DeviceWithNetworks

@Dao
interface DeviceDao {
    @Query("SELECT * FROM RemoteDeviceEntity")
    suspend fun getAllDevices(): List<RemoteDeviceEntity>

    @Query("SELECT * FROM RemoteDeviceEntity")
    fun getAllDevicesFlow(): Flow<List<RemoteDeviceEntity>>

    @Query("SELECT * FROM RemoteDeviceEntity WHERE deviceId = :deviceId")
    fun getRemoteDevice(deviceId: String): Flow<RemoteDeviceEntity?>

    @Query("SELECT * FROM RemoteDeviceEntity ORDER BY lastConnected DESC LIMIT 1")
    fun getLastConnectedDevice(): RemoteDeviceEntity?

    @Query("SELECT * FROM RemoteDeviceEntity ORDER BY lastConnected DESC LIMIT 1")
    fun getLastConnectedDeviceFlow(): Flow<RemoteDeviceEntity?>

    @Query("DELETE FROM REMOTEDEVICEENTITY WHERE deviceId = :deviceId")
    suspend fun removeDevice(deviceId: String)

    @Update
    suspend fun updateDevice(device: RemoteDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDevice(device: RemoteDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocalDevice(device: LocalDeviceEntity)

    @Update
    suspend fun updateLocalDevice(device: LocalDeviceEntity)

    @Query("SELECT * FROM LocalDeviceEntity LIMIT 1")
    suspend fun getLocalDevice(): LocalDeviceEntity

    @Query("UPDATE RemoteDeviceEntity SET prefAddress = :preferredIp WHERE deviceId = :deviceId")
    suspend fun updatePreferredIp(deviceId: String, preferredIp: String)

    @Query("UPDATE RemoteDeviceEntity SET ipAddresses = :ipAddresses WHERE deviceId = :deviceId")
    suspend fun updateIpAddresses(deviceId: String, ipAddresses: List<String>)

    @Transaction
    @Query("SELECT * FROM RemoteDeviceEntity WHERE deviceId = :deviceId")
    fun getDeviceWithNetworks(deviceId: String): Flow<DeviceWithNetworks?>

    @Transaction
    @Query("SELECT * FROM RemoteDeviceEntity")
    fun getAllDevicesWithNetworks(): Flow<List<DeviceWithNetworks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNetworkToDevice(crossRef: DeviceNetworkCrossRef)

    @Query("DELETE FROM DeviceNetworkCrossRef WHERE deviceId = :deviceId AND ssid = :ssid")
    suspend fun removeNetworkFromDevice(deviceId: String, ssid: String)

    @Query("DELETE FROM DeviceNetworkCrossRef WHERE deviceId = :deviceId")
    suspend fun removeAllNetworksFromDevice(deviceId: String)

    @Query("SELECT n.* FROM NetworkEntity n INNER JOIN DeviceNetworkCrossRef ref ON n.ssid = ref.ssid WHERE ref.deviceId = :deviceId")
    fun getNetworksForDevice(deviceId: String): Flow<List<NetworkEntity>>
}