package com.example.customviewsample.utils

import com.example.customviewsample.R
import com.example.customviewsample.data.CanvasSize

fun getCanvasSizeList(): List<CanvasSize> {
    val canvasSizeList = mutableListOf<CanvasSize>()
    canvasSizeList.add(CanvasSize(width = 2016, height = 1512, iconRes = R.drawable.ic_picture_landscape, title = "Landscape", isTint = true))
    canvasSizeList.add(CanvasSize(width = 1512, height = 4016, iconRes = R.drawable.ic_picture_portrait, title = "Portrait", isTint = true))
    canvasSizeList.add(CanvasSize(width = 1512, height = 1512, iconRes = R.drawable.ic_picture, title = "Square", isTint = true))
    canvasSizeList.add(CanvasSize(width = 1080, height = 1920, iconRes = R.drawable.ic_size_instagram, title = "Instagram Story"))
    canvasSizeList.add(CanvasSize(width = 1080, height = 1080, iconRes = R.drawable.ic_size_instagram, title = "Instagram Post"))
    canvasSizeList.add(CanvasSize(width = 1080, height = 1920, iconRes = R.drawable.ic_size_instagram, title = "Instagram Reel"))
    canvasSizeList.add(CanvasSize(width = 1080, height = 1920, iconRes = R.drawable.pictogram_tiktok, title = "TikTok"))
    canvasSizeList.add(CanvasSize(width = 1080, height = 1440, iconRes = R.drawable.pictogram_tiktok, title = "TikTok Thumbnail"))
    canvasSizeList.add(CanvasSize(width = 1280, height = 720, iconRes = R.drawable.pictogram_youtube, title = "Youtube Cover"))
    canvasSizeList.add(CanvasSize(width = 2560, height = 1440, iconRes = R.drawable.pictogram_youtube, title = "Youtube Channel Art"))
    canvasSizeList.add(CanvasSize(width = 820, height = 132, iconRes = R.drawable.pictogram_facebook, title = "Facebook Cover"))
    canvasSizeList.add(CanvasSize(width = 1200, height = 628, iconRes = R.drawable.pictogram_facebook, title = "Facebook Post"))
    canvasSizeList.add(CanvasSize(width = 1080, height = 1080, iconRes = R.drawable.pictogram_facebook, title = "Facebook Marketplace"))
    canvasSizeList.add(CanvasSize(width = 725, height = 1102, iconRes = R.drawable.pictogram_pinterest, title = "Pinterest"))
    canvasSizeList.add(CanvasSize(width = 820, height = 312, iconRes = R.drawable.pictogram_linkedin, title = "LinkedIn Banner"))
    canvasSizeList.add(CanvasSize(width = 800, height = 800, iconRes = R.drawable.pictogram_linkedin, title = "LinkedIn Profile Pic"))
    canvasSizeList.add(CanvasSize(width = 512, height = 512, iconRes = R.drawable.pictogram_whatsapp, title = "WhatsApp Sticker"))
    // Ebay
    canvasSizeList.add(CanvasSize(width = 1600, height = 1600, iconRes = R.drawable.pictogram_ebay, title = "Ebay"))
    // Poshmark
    canvasSizeList.add(CanvasSize(width = 1080, height = 1080, iconRes = R.drawable.pictogram_poshmark, title = "Poshmark"))
    // Etsy
    canvasSizeList.add(CanvasSize(width = 2700, height = 2025, iconRes = R.drawable.pictogram_etsy, title = "Etsy"))
    // Depop
    canvasSizeList.add(CanvasSize(width = 1280, height = 1280, iconRes = R.drawable.pictogram_depop, title = "Depop"))
    // Mercari
    canvasSizeList.add(CanvasSize(width = 1080, height = 1080, iconRes = R.drawable.pictogram_mercari, title = "Mercari"))
    // Amazon
    canvasSizeList.add(CanvasSize(width = 2000, height = 2000, iconRes = R.drawable.pictogram_amazon, title = "Amazon"))
    // Mercado Libre
    canvasSizeList.add(CanvasSize(width = 1200, height = 1200, iconRes = R.drawable.pictogram_mercadolibre, title = "Mercado Libre"))
    // Vinted
    canvasSizeList.add(CanvasSize(width = 800, height = 600, iconRes = R.drawable.pictogram_vinted, title = "Vinted"))
    // Shopee
    canvasSizeList.add(CanvasSize(width = 1080, height = 1080, iconRes = R.drawable.pictogram_shopee, title = "Shopee"))
    // Shopify Landscape
    canvasSizeList.add(CanvasSize(width = 2000, height = 1800, iconRes = R.drawable.pictogram_shopify, title = "Shopify Landscape"))
    // Shopify Portrait
    canvasSizeList.add(CanvasSize(width = 1600, height = 2000, iconRes = R.drawable.pictogram_shopify, title = "Shopify Portrait"))
    // Shopify Square
    canvasSizeList.add(CanvasSize(width = 2048, height = 2048, iconRes = R.drawable.pictogram_shopify, title = "Shopify Square"))
    return canvasSizeList
}