package com.nutrition.scanner.data

data class NutritionInfo(
    val foodName: String,
    val calories: Int,      // kcal per 100g
    val protein: Float,     // g per 100g
    val carbs: Float,       // g per 100g
    val fat: Float,         // g per 100g
    val fiber: Float,       // g per 100g
    val sugar: Float,       // g per 100g
    val sodium: Float,      // mg per 100g
    val servingSize: String = "100g",
    val confidence: Float = 1.0f
)

data class ScanResult(
    val id: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val nutrition: NutritionInfo,
    val timestamp: Long = System.currentTimeMillis()
)

object FoodDatabase {
    private val foods = mapOf(
        // Thai food
        "ข้าวมันไก่" to NutritionInfo("ข้าวมันไก่", 165, 13f, 18f, 5f, 0.3f, 1f, 380f),
        "ข้าวผัด" to NutritionInfo("ข้าวผัด", 170, 5f, 28f, 4f, 0.5f, 1.5f, 450f),
        "ผัดไทย" to NutritionInfo("ผัดไทย", 150, 8f, 20f, 4f, 1f, 8f, 500f),
        "ต้มยำกุ้ง" to NutritionInfo("ต้มยำกุ้ง", 65, 8f, 5f, 1.5f, 0.5f, 2f, 800f),
        "ส้มตำ" to NutritionInfo("ส้มตำ", 50, 2f, 10f, 0.5f, 2f, 6f, 600f),
        "แกงเขียวหวาน" to NutritionInfo("แกงเขียวหวาน", 120, 10f, 5f, 7f, 1f, 2f, 400f),
        "มัสมั่น" to NutritionInfo("แกงมัสมั่น", 140, 12f, 6f, 8f, 1.5f, 3f, 350f),
        "ข้าวเหนียวมะม่วง" to NutritionInfo("ข้าวเหนียวมะม่วง", 220, 4f, 42f, 5f, 1f, 25f, 150f),
        "ก๋วยเตี๋ยว" to NutritionInfo("ก๋วยเตี๋ยว", 110, 5f, 18f, 2f, 0.5f, 1f, 500f),
        "ข้าวขาหมู" to NutritionInfo("ข้าวขาหมู", 180, 15f, 20f, 5f, 0.3f, 2f, 500f),
        "ข้าวคลุกกะปิ" to NutritionInfo("ข้าวคลุกกะปิ", 155, 6f, 25f, 3f, 0.5f, 1f, 600f),
        "ลาบ" to NutritionInfo("ลาบ", 120, 15f, 5f, 4f, 0.5f, 2f, 450f),
        "น้ำตก" to NutritionInfo("น้ำตก", 100, 12f, 4f, 3f, 0.5f, 1.5f, 400f),
        "ข้าวสวย" to NutritionInfo("ข้าวสวย", 130, 2.7f, 28f, 0.3f, 0.4f, 0f, 1f),
        "ไข่ดาว" to NutritionInfo("ไข่ดาว", 190, 13f, 1f, 15f, 0f, 1f, 200f),
        "ไข่เจียว" to NutritionInfo("ไข่เจียว", 200, 14f, 2f, 16f, 0f, 1f, 250f),
        "ผักบุ้ง" to NutritionInfo("ผัดผักบุ้ง", 45, 2f, 5f, 2f, 1.5f, 1f, 300f),
        "ผัดผัก" to NutritionInfo("ผัดผักรวม", 50, 2f, 6f, 2f, 2f, 2f, 250f),
        "ต้มข่าไก่" to NutritionInfo("ต้มข่าไก่", 80, 9f, 4f, 3f, 0.5f, 1f, 350f),
        "ยำวุ้นเส้น" to NutritionInfo("ยำวุ้นเส้น", 90, 5f, 15f, 1.5f, 0.5f, 3f, 400f),

        // International food
        "pizza" to NutritionInfo("Pizza", 266, 11f, 33f, 10f, 2.3f, 3.6f, 600f),
        "burger" to NutritionInfo("Burger", 295, 17f, 24f, 14f, 1.3f, 5f, 410f),
        "sushi" to NutritionInfo("Sushi", 150, 6f, 28f, 1.5f, 0.8f, 8f, 300f),
        "ramen" to NutritionInfo("Ramen", 180, 8f, 22f, 7f, 1f, 1f, 900f),
        "salad" to NutritionInfo("Salad", 35, 2f, 5f, 0.5f, 2f, 3f, 50f),
        "steak" to NutritionInfo("Steak", 270, 26f, 0f, 18f, 0f, 0f, 60f),
        "fried chicken" to NutritionInfo("Fried Chicken", 260, 20f, 8f, 16f, 0.3f, 0f, 600f),
        "pasta" to NutritionInfo("Pasta", 160, 6f, 30f, 1.5f, 1.8f, 1f, 5f),
        "sandwich" to NutritionInfo("Sandwich", 250, 12f, 28f, 9f, 2f, 4f, 500f),
        "fried rice" to NutritionInfo("Fried Rice", 170, 5f, 28f, 4f, 0.5f, 1.5f, 450f),
        "noodles" to NutritionInfo("Noodles", 140, 5f, 25f, 2f, 1f, 1f, 400f),
        "soup" to NutritionInfo("Soup", 45, 3f, 5f, 1f, 0.5f, 1.5f, 350f),
        "curry" to NutritionInfo("Curry", 120, 8f, 8f, 6f, 1.5f, 3f, 400f),
        "grilled chicken" to NutritionInfo("Grilled Chicken", 165, 31f, 0f, 3.6f, 0f, 0f, 75f),
        "fish" to NutritionInfo("Fish", 130, 22f, 0f, 4f, 0f, 0f, 60f),
        "egg" to NutritionInfo("Egg", 155, 13f, 1.1f, 11f, 0f, 1.1f, 124f),
        "bread" to NutritionInfo("Bread", 265, 9f, 49f, 3.2f, 2.7f, 5f, 490f),
        "rice" to NutritionInfo("Rice", 130, 2.7f, 28f, 0.3f, 0.4f, 0f, 1f),
        "apple" to NutritionInfo("Apple", 52, 0.3f, 14f, 0.2f, 2.4f, 10f, 1f),
        "banana" to NutritionInfo("Banana", 89, 1.1f, 23f, 0.3f, 2.6f, 12f, 1f),
        "orange" to NutritionInfo("Orange", 47, 0.9f, 12f, 0.1f, 2.4f, 9f, 0f),
        "mango" to NutritionInfo("Mango", 60, 0.8f, 15f, 0.4f, 1.6f, 14f, 1f),
        "watermelon" to NutritionInfo("Watermelon", 30, 0.6f, 8f, 0.2f, 0.4f, 6f, 1f),
        "coffee" to NutritionInfo("Coffee", 2, 0.3f, 0f, 0f, 0f, 0f, 5f),
        "tea" to NutritionInfo("Tea", 1, 0f, 0.3f, 0f, 0f, 0f, 1f),
        "milk" to NutritionInfo("Milk", 42, 3.4f, 5f, 1f, 0f, 5f, 44f),
        "yogurt" to NutritionInfo("Yogurt", 59, 10f, 3.6f, 0.4f, 0f, 3.6f, 36f),
        "ice cream" to NutritionInfo("Ice Cream", 207, 3.5f, 24f, 11f, 0.7f, 21f, 80f),
        "cake" to NutritionInfo("Cake", 350, 5f, 50f, 15f, 1f, 30f, 300f),
        "chocolate" to NutritionInfo("Chocolate", 546, 5f, 60f, 31f, 7f, 48f, 24f),
        "chips" to NutritionInfo("Chips", 536, 7f, 53f, 35f, 4.8f, 0.3f, 530f),
        "donut" to NutritionInfo("Donut", 400, 5f, 51f, 21f, 1.5f, 25f, 350f)
    )

    fun findFood(query: String): NutritionInfo? {
        val lowerQuery = query.lowercase().trim()
        // Exact match
        foods[lowerQuery]?.let { return it }
        // Partial match
        for ((key, value) in foods) {
            if (lowerQuery.contains(key) || key.contains(lowerQuery)) {
                return value
            }
        }
        return null
    }

    fun searchFoods(query: String): List<NutritionInfo> {
        val lowerQuery = query.lowercase().trim()
        return foods.filter { (key, _) ->
            key.contains(lowerQuery) || lowerQuery.contains(key)
        }.values.toList()
    }
}
