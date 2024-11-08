package com.mogun.deliciousfinderapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.mogun.deliciousfinderapp.databinding.ActivityMainBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var naverMap: NaverMap
    private var isMapInit = false

    private var restaurantListAdapter = RestaurantListAdapter {
        collapseBottomSheet()   // BottomSheet 내리기
        moveCamera(it, 17.0)
    }

    private var markers = emptyList<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        binding.bottomSheetLayout.searchResultRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = restaurantListAdapter
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.isNotEmpty() == true) {
                    SearchRepository.getDeliciousRestaurant(query)
                        .enqueue(object : Callback<SearchResult> {
                            override fun onResponse(
                                call: Call<SearchResult>,
                                response: Response<SearchResult>
                            ) {
                                // 검색 목록 데이터
                                val searchItemList = response.body()?.items.orEmpty()

                                if (searchItemList.isEmpty()) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "검색 결과가 없습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return
                                } else if (!isMapInit) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "오류가 발생했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return
                                }

                                // 이전 마커 초기화
                                markers.forEach {
                                    it.map = null
                                }

                                // 마커 추가
                                markers = searchItemList.map {
                                    Marker(
                                        LatLng(
                                            it.mapy.toDouble() / 1_000_0000,
                                            it.mapx.toDouble()/ 1_000_0000,
                                        )
                                    ).apply {
                                        captionText = it.title
                                        map = naverMap
                                    }
                                }

                                // 검색 결과 업데이트
                                restaurantListAdapter.setData(searchItemList)

                                collapseBottomSheet()   // BottomSheet 내리기

                                // 검색 결과의 첫번째 결과 값으로 position 이동
                                moveCamera(markers.first().position, 13.0)
                            }

                            override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                                Log.d("MainActivity", "onFailure: $t")
                            }
                        })

                    return false
                } else {
                    return true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    // 맵 객체를 얻어온 뒤에 추후 동작들이 가능하다.
    override fun onMapReady(mapObject: NaverMap) {
        naverMap = mapObject
        isMapInit = true
    }

    fun moveCamera(position: LatLng, zoomLevel: Double) {
        if (isMapInit.not()) return

        val cameraUpdate = CameraUpdate.scrollAndZoomTo(position, zoomLevel)
            .animate(CameraAnimation.Easing)
        naverMap.moveCamera(cameraUpdate)
    }

    private fun collapseBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout.root)
        bottomSheetBehavior.state = STATE_COLLAPSED
    }
}