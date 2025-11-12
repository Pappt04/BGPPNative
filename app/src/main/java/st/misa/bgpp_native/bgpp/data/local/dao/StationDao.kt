package st.misa.bgpp_native.bgpp.data.local.dao

import androidx.room.*
import st.misa.bgpp_native.bgpp.data.local.dto.StationDbDto
import st.misa.bgpp_native.bgpp.domain.model.City

@Dao
interface StationDao {

    @Query("""
        SELECT * FROM stations
        WHERE city = :city AND id LIKE :query || '%'
        ORDER BY id ASC
    """)
    suspend fun searchStationsByIdPrefix(city: String, query: String): List<StationDbDto>

//    @Query("""
//        SELECT * FROM stations
//        WHERE id IN (
//            SELECT rowid FROM stations_fts
//            WHERE stations_fts MATCH :query
//        )
//        AND city = :city
//    """)
    @Query("""
        SELECT * FROM stations
        WHERE city = :city
          AND name LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    suspend fun searchStationsByName(city : String, query: String): List<StationDbDto>

    @Query("""
        SELECT * FROM stations
        WHERE city = :city
          AND lat BETWEEN :minLat AND :maxLat
          AND lon BETWEEN :minLon AND :maxLon
          ORDER BY abs(lat * lon * 1000000) % 7919 -- uniformly space selection on map
        LIMIT :limit
    """)
    suspend fun findStationsInBoundingBox(
        city: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int
    ): List<StationDbDto>

    @Query("SELECT * FROM stations WHERE city = :city AND favorite = 1")
    suspend fun findFavoriteStations(city: String): List<StationDbDto>

    @Query("SELECT * FROM stations WHERE city = :city AND id = :stationId LIMIT 1")
    suspend fun getStationById(city: String, stationId: String): StationDbDto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationDbDto>)

    @Update
    suspend fun updateStation(station: StationDbDto)

    @Query("DELETE FROM stations WHERE city = :city")
    suspend fun deleteStationsForCity(city: String)

    @Query("INSERT INTO stations_fts(stations_fts) VALUES('rebuild')")
    suspend fun rebuildFts()

    @Transaction
    suspend fun insertAndRebuild(stations: List<StationDbDto>) {
        insertStations(stations)
        rebuildFts()
    }

    @Transaction
    suspend fun updateAndRebuild(station: StationDbDto) {
        updateStation(station)
        rebuildFts()
    }

    @Query("UPDATE stations SET favorite = CASE WHEN favorite = 1 THEN 0 ELSE 1 END WHERE id = :stationId AND city = :city")
    suspend fun toggleFavorite(city: String, stationId: String )

}
