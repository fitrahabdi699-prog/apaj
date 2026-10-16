package com.example.util

import com.example.data.model.UserLocation

object IndonesianCities {
    val CITIES = listOf(
        UserLocation(-6.2088, 106.8456, "Jakarta (DKI Jakarta)", false),
        UserLocation(-6.9175, 107.6191, "Bandung (Jawa Barat)", false),
        UserLocation(-7.2575, 112.7521, "Surabaya (Jawa Timur)", false),
        UserLocation(-6.9667, 110.4167, "Semarang (Jawa Tengah)", false),
        UserLocation(-7.7956, 110.3695, "Yogyakarta (DIY)", false),
        UserLocation(3.5952, 98.6722, "Medan (Sumatera Utara)", false),
        UserLocation(-0.9471, 100.4172, "Padang (Sumatera Barat)", false),
        UserLocation(5.5483, 95.3238, "Banda Aceh (Aceh)", false),
        UserLocation(-2.9761, 104.7754, "Palembang (Sumatera Selatan)", false),
        UserLocation(-5.4297, 105.2625, "Bandar Lampung (Lampung)", false),
        UserLocation(-8.6705, 115.2126, "Denpasar (Bali)", false),
        UserLocation(-8.5833, 116.1167, "Mataram (Lombok / NTB)", false),
        UserLocation(-10.1772, 123.6070, "Kupang (NTT)", false),
        UserLocation(-5.1477, 119.4327, "Makassar (Sulawesi Selatan)", false),
        UserLocation(1.4748, 124.8421, "Manado (Sulawesi Utara)", false),
        UserLocation(-0.9003, 119.8779, "Palu (Sulawesi Tengah)", false),
        UserLocation(-3.6547, 128.1906, "Ambon (Maluku)", false),
        UserLocation(-2.5489, 140.7181, "Jayapura (Papua)", false),
        UserLocation(-0.0263, 109.3425, "Pontianak (Kalimantan Barat)", false),
        UserLocation(-3.3194, 114.5908, "Banjarmasin (Kalimantan Selatan)", false),
        UserLocation(-1.2654, 116.8312, "Balikpapan (Kalimantan Timur)", false),
        UserLocation(-0.5016, 117.1265, "Samarinda (Kalimantan Timur)", false)
    )

    val DEFAULT_CITY = CITIES[0] // Jakarta
}
