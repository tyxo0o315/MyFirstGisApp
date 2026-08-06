package io.github.tyxo0o315.myfirstgisapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.data.ArcGISFeature
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geometry.AngularUnit
import com.esri.arcgisruntime.geometry.AngularUnitId
import com.esri.arcgisruntime.geometry.GeodeticCurveType
import com.esri.arcgisruntime.geometry.Geometry
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.LinearUnit
import com.esri.arcgisruntime.geometry.LinearUnitId
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReference
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleRenderer
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.roundToInt

private data class PoiCategory(
    val name: String,
    val expression: String,
    val color: Int,
    val outlineColor: Int
)

private data class BookmarkItem(val name: String, val point: Point, val graphic: Graphic)

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var map: ArcGISMap

    private val featureLayers = mutableListOf<FeatureLayer>()
    private val originalExpressions = mutableListOf<String>()

    private val categories = listOf(
        PoiCategory("地名地址", "type LIKE '地名地址信息%'", Color.DKGRAY, Color.WHITE),
        PoiCategory("通行设施", "type LIKE '通行设施%'", Color.YELLOW, Color.BLACK),
        PoiCategory("商务住宅", "type LIKE '商务住宅%'", Color.rgb(139, 69, 19), Color.WHITE),
        PoiCategory("购物服务", "type LIKE '购物服务%'", Color.rgb(255, 165, 0), Color.WHITE),
        PoiCategory("餐饮服务", "type LIKE '餐饮服务%'", Color.RED, Color.WHITE),
        PoiCategory("公司企业", "type LIKE '公司企业%'", Color.GRAY, Color.BLACK),
        PoiCategory("住宿服务", "type LIKE '住宿服务%'", Color.rgb(128, 0, 128), Color.WHITE),
        PoiCategory("生活服务", "type LIKE '生活服务%'", Color.rgb(0, 128, 0), Color.WHITE),
    )

    private val basemapOptions = listOf(
        BasemapStyle.ARCGIS_TOPOGRAPHIC to "地形图",
        BasemapStyle.ARCGIS_IMAGERY to "卫星图",
        BasemapStyle.ARCGIS_STREETS to "街道图",
        BasemapStyle.ARCGIS_DARK_GRAY to "暗色图"
    )
    private var currentBasemapIndex = 0
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private val fieldLabels = mapOf(
        "type" to "类型",
        "address" to "地址",
        "tel" to "电话",
        "postcode" to "邮编",
        "city" to "城市",
        "district" to "区县",
        "pname" to "省份",
        "adname" to "行政区"
    )

    // 功能A：地图书签叠加层 + 书签列表
    private val bufferOverlay = GraphicsOverlay()
    private val bookmarks = mutableListOf<BookmarkItem>()

    // 点击反馈：指示环 + 选中高亮
    private val selectionOverlay = GraphicsOverlay()

    private lateinit var loadingCard: androidx.cardview.widget.CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configureArcGisApiKey()

        loadingCard = findViewById(R.id.loadingCard)
        mapView = findViewById(R.id.mapView)
        map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
        mapView.map = map
        mapView.graphicsOverlays.add(bufferOverlay)
        mapView.graphicsOverlays.add(selectionOverlay)
        mapView.setViewpointCenterAsync(Point(108.94, 34.29, SpatialReference.create(4326)), 50000.0)

        val serviceUrl = "https://services8.arcgis.com/YcT8zr575DqanWh7/arcgis/rest/services/POI-city/FeatureServer/0"
        categories.forEach { cat ->
            val layer = FeatureLayer(ServiceFeatureTable(serviceUrl))
            layer.definitionExpression = cat.expression
            val symbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, cat.color, 10f)
            symbol.outline = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, cat.outlineColor, 1.5f)
            layer.renderer = SimpleRenderer(symbol)
            map.operationalLayers.add(layer)
            featureLayers.add(layer)
            originalExpressions.add(cat.expression)
        }

        setupZoomButtons()
        setupLayerPanel()
        setupBasemapButton()
        setupSearch()
        setupIdentifyListener()
        setupStatsButton()
        requestLocationPermission()
        setupMyLocationButton()
    }

    private fun configureArcGisApiKey() {
        if (BuildConfig.ARCGIS_API_KEY.isBlank()) {
            Log.w("MyFirstGisApp", "ArcGIS API key is not configured in local.properties")
            Toast.makeText(this, "ArcGIS API Key 未配置，底图服务可能无法加载", Toast.LENGTH_LONG).show()
            return
        }
        ArcGISRuntimeEnvironment.setApiKey(BuildConfig.ARCGIS_API_KEY)
    }

    private fun setupZoomButtons() {
        findViewById<Button>(R.id.zoomIn).setOnClickListener {
            mapView.setViewpointScaleAsync(mapView.mapScale / 2)
        }
        findViewById<Button>(R.id.zoomOut).setOnClickListener {
            mapView.setViewpointScaleAsync(mapView.mapScale * 2)
        }
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            mapView.setViewpointCenterAsync(
                Point(108.94, 34.29, SpatialReference.create(4326)), 50000.0
            )
        }
    }

    private fun setupLayerPanel() {
        val root = findViewById<ConstraintLayout>(R.id.main)
        val btnLayers = findViewById<Button>(R.id.btnLayers)
        val layerPanel = findViewById<CardView>(R.id.layerPanel)
        val layerHeader = findViewById<LinearLayout>(R.id.layerHeader)
        val checkboxContainer = findViewById<LinearLayout>(R.id.checkboxContainer)
        val tvChevron = findViewById<TextView>(R.id.tvChevron)
        var isExpanded = true

        btnLayers.setOnClickListener {
            TransitionManager.beginDelayedTransition(root, AutoTransition().apply { duration = 200 })
            layerPanel.visibility = if (layerPanel.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        layerHeader.setOnClickListener {
            isExpanded = !isExpanded
            TransitionManager.beginDelayedTransition(root, AutoTransition().apply { duration = 180 })
            checkboxContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            tvChevron.animate().rotation(if (isExpanded) 0f else 180f).setDuration(180).start()
        }

        val dp = resources.displayMetrics.density
        val mintColor = ContextCompat.getColor(this, R.color.accent_mint)
        val switchStates = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val thumbTints = ColorStateList(switchStates, intArrayOf(mintColor, 0xFFBBBBBB.toInt()))
        val trackTints = ColorStateList(switchStates, intArrayOf(
            Color.argb(100, 0, 229, 160), Color.parseColor("#2A3D50")
        ))

        categories.forEachIndexed { index, cat ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, (5 * dp).toInt(), 0, (5 * dp).toInt())
            }

            val dotSize = (10 * dp).toInt()
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).also {
                    it.marginEnd = (10 * dp).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(cat.color)
                }
            }

            val label = TextView(this).apply {
                text = cat.name
                textSize = 13f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val toggle = SwitchCompat(this).apply {
                isChecked = featureLayers[index].isVisible
                thumbTintList = thumbTints
                trackTintList = trackTints
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnCheckedChangeListener { _, checked ->
                    featureLayers[index].isVisible = checked
                }
            }

            row.addView(dot)
            row.addView(label)
            row.addView(toggle)
            checkboxContainer.addView(row)
        }
    }

    private fun setupBasemapButton() {
        val btnBasemap = findViewById<Button>(R.id.btnBasemap)
        btnBasemap.setOnClickListener {
            val popup = PopupMenu(this, btnBasemap)
            basemapOptions.forEachIndexed { index, (_, name) ->
                val title = if (index == currentBasemapIndex) "✓ $name" else "  $name"
                popup.menu.add(0, index, index, title)
            }
            popup.setOnMenuItemClickListener { item ->
                currentBasemapIndex = item.itemId
                val (style, name) = basemapOptions[currentBasemapIndex]
                map.basemap = Basemap(style)
                btnBasemap.text = name
                true
            }
            popup.show()
        }
    }

    private fun setupSearch() {
        val searchInput = findViewById<EditText>(R.id.searchInput)
        val btnClear = findViewById<ImageButton>(R.id.btnClearSearch)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchInput.setOnEditorActionListener { _, _, _ ->
            val keyword = searchInput.text.toString().trim()
            applySearch(keyword)
            searchAndNavigate(keyword)
            true
        }

        btnClear.setOnClickListener {
            searchInput.text.clear()
            applySearch("")
            featureLayers.forEach { it.clearSelection() }
        }
    }

    private fun applySearch(keyword: String) {
        val sanitized = keyword.replace("'", "''")
        featureLayers.forEachIndexed { index, layer ->
            layer.definitionExpression = if (sanitized.isEmpty()) {
                originalExpressions[index]
            } else {
                "${originalExpressions[index]} AND name LIKE '%$sanitized%'"
            }
        }
    }

    private fun searchAndNavigate(keyword: String) {
        featureLayers.forEach { it.clearSelection() }
        if (keyword.isEmpty()) return

        val sanitized = keyword.replace("'", "''")
        val params = QueryParameters().apply {
            whereClause = "UPPER(name) LIKE '%${sanitized.uppercase()}%'"
            isReturnGeometry = true
        }

        val visibleLayers = featureLayers.filter { it.isVisible }
        if (visibleLayers.isEmpty()) return
        showLoading()

        val pendingCount = AtomicInteger(visibleLayers.size)
        val firstGeometry = AtomicReference<Geometry?>(null)

        visibleLayers.forEach { layer ->
            val future = layer.selectFeaturesAsync(params, FeatureLayer.SelectionMode.NEW)
            future.addDoneListener {
                if (isFinishing || isDestroyed) return@addDoneListener
                try {
                    val iterator = future.get().iterator()
                    if (iterator.hasNext()) {
                        val geom = iterator.next().geometry
                        if (geom != null) firstGeometry.compareAndSet(null, geom)
                    }
                } catch (ex: Exception) {
                    Log.e("SearchNav", "查询失败: ${ex.message}")
                } finally {
                    if (pendingCount.decrementAndGet() == 0) {
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            hideLoading()
                            val geom = firstGeometry.get()
                            if (geom != null) {
                                if (geom is Point) {
                                    mapView.setViewpointCenterAsync(geom, 5000.0)
                                } else {
                                    mapView.setViewpointGeometryAsync(geom.extent, 200.0)
                                }
                            } else {
                                Toast.makeText(this, "未找到「$keyword」相关地点", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupIdentifyListener() {
        mapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, mapView) {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val screenPoint = android.graphics.Point(e.x.roundToInt(), e.y.roundToInt())

                // 先检查是否点击了书签（本地同步判断，无网络延迟）
                val tappedBookmark = bookmarks.firstOrNull { bkm ->
                    val scr = mapView.locationToScreen(bkm.point)
                    kotlin.math.hypot((scr.x - e.x).toDouble(), (scr.y - e.y).toDouble()) < 36.0
                }
                if (tappedBookmark != null) {
                    selectionOverlay.graphics.clear()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("📌 ${tappedBookmark.name}")
                        .setItems(arrayOf("飞行到此", "删除书签")) { _, which ->
                            when (which) {
                                0 -> mapView.setViewpointCenterAsync(tappedBookmark.point, 5000.0)
                                1 -> {
                                    bufferOverlay.graphics.remove(tappedBookmark.graphic)
                                    bookmarks.remove(tappedBookmark)
                                    Toast.makeText(this@MainActivity, "书签已删除", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    return true
                }

                // 普通点击：清除选中 + 画指示环 + 识别 POI
                selectionOverlay.graphics.clear()
                featureLayers.forEach { it.clearSelection() }
                val tapPoint = mapView.screenToLocation(screenPoint)
                val tapSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.WHITE, 20f).apply {
                    outline = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.rgb(0, 140, 255), 3f)
                }
                selectionOverlay.graphics.add(Graphic(tapPoint, tapSymbol))

                val future = mapView.identifyLayersAsync(screenPoint, 10.0, false, -1)
                future.addDoneListener {
                    try {
                        val results = future.get()
                        runOnUiThread { handleIdentifyResults(results) }
                    } catch (ex: Exception) {
                        Log.e("IdentifyError", ex.message ?: "识别查询出错")
                        runOnUiThread { selectionOverlay.graphics.clear() }
                    }
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                val screenPoint = android.graphics.Point(e.x.roundToInt(), e.y.roundToInt())
                val mapPoint = mapView.screenToLocation(screenPoint)
                runOnUiThread { showAddBookmarkDialog(mapPoint) }
            }
        }
    }

    private fun handleIdentifyResults(results: List<IdentifyLayerResult>) {
        if (isFinishing || isDestroyed) return
        selectionOverlay.graphics.clear()   // 清除指示环（无论是否命中）
        if (results.isEmpty()) return
        val firstResult = results[0]
        val elements = firstResult.elements
        if (elements.isEmpty()) return
        val feature = elements[0] as? ArcGISFeature ?: return
        val name = feature.attributes["name"]?.toString()
        if (name.isNullOrEmpty() || name == "null") return

        // 高亮命中的 POI（SDK 内置青色光晕）
        val layer = firstResult.layerContent as? FeatureLayer
        layer?.selectFeature(feature)

        val catIndex = if (layer != null) featureLayers.indexOf(layer) else -1
        val category = if (catIndex >= 0) categories[catIndex] else null
        showPoiBottomSheet(feature, category, onDismiss = { layer?.clearSelection() })
    }

    private fun showPoiBottomSheet(
        feature: ArcGISFeature,
        category: PoiCategory? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_poi, null)
        val dp = resources.displayMetrics.density

        // 分类标识行
        if (category != null) {
            view.findViewById<View>(R.id.vCategoryDot).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(category.color)
            }
            view.findViewById<TextView>(R.id.tvCategoryName).text = category.name
        } else {
            view.findViewById<LinearLayout>(R.id.llCategoryBadge).visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.tvPoiTitle).text =
            feature.attributes["name"]?.toString() ?: "未知POI"

        val llAttributes = view.findViewById<LinearLayout>(R.id.llAttributes)
        val labelWidth = (72 * dp).toInt()
        val rowPad = (6 * dp).toInt()

        feature.attributes.forEach { (key, value) ->
            if (key == "name" || value == null) return@forEach
            val valueStr = value.toString()
            if (valueStr.isEmpty() || valueStr == "null") return@forEach
            val label = fieldLabels[key] ?: return@forEach

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, rowPad, 0, rowPad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val tvLabel = TextView(this).apply {
                text = "$label："
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_hint))
                layoutParams = LinearLayout.LayoutParams(labelWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val tvValue = TextView(this).apply {
                text = valueStr
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvLabel)
            row.addView(tvValue)
            llAttributes.addView(row)
        }

        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_deep))
        }
        onDismiss?.let { cb -> dialog.setOnDismissListener { cb() } }
        dialog.setContentView(view)
        dialog.show()
    }

    // ─── 功能A：地图书签 ────────────────────────────────────────────────────

    private fun showAddBookmarkDialog(mapPoint: Point) {
        val dp = resources.displayMetrics.density
        val input = android.widget.EditText(this).apply {
            hint = "输入书签名称（可不填）"
            isSingleLine = true
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
        }
        AlertDialog.Builder(this)
            .setTitle("添加书签")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "书签 ${bookmarks.size + 1}" }
                addBookmark(mapPoint, name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addBookmark(point: Point, name: String) {
        val symbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.rgb(255, 200, 0), 22f).apply {
            outline = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.WHITE, 2f)
        }
        val graphic = Graphic(point, symbol)
        graphic.attributes["bkmName"] = name
        bufferOverlay.graphics.add(graphic)
        bookmarks.add(BookmarkItem(name, point, graphic))
        Toast.makeText(this, "已标记：$name", Toast.LENGTH_SHORT).show()
    }


    // ─── 功能D：视野内 POI 统计仪表盘 ────────────────────────────────────────────

    private fun setupStatsButton() {
        findViewById<Button>(R.id.btnStats).setOnClickListener {
            queryAndShowStats()
        }
    }

    private fun queryAndShowStats() {
        val extent = mapView.visibleArea ?: return
        showLoading()

        val counts = arrayOfNulls<Long>(featureLayers.size)
        val pendingCount = AtomicInteger(featureLayers.size)

        featureLayers.forEachIndexed { index, layer ->
            if (!layer.isVisible) {
                counts[index] = -1L
                if (pendingCount.decrementAndGet() == 0) {
                    runOnUiThread { showStatsSheet(counts.map { it ?: 0L }) }
                }
                return@forEachIndexed
            }

            val params = QueryParameters().apply {
                geometry = extent
                spatialRelationship = QueryParameters.SpatialRelationship.INTERSECTS
            }

            val future = layer.featureTable.queryFeatureCountAsync(params)
            future.addDoneListener {
                try {
                    counts[index] = future.get()
                } catch (ex: Exception) {
                    Log.e("StatsQuery", "图层 $index 计数失败: ${ex.message}")
                    counts[index] = 0L
                } finally {
                    if (pendingCount.decrementAndGet() == 0) {
                        runOnUiThread { showStatsSheet(counts.map { it ?: 0L }) }
                    }
                }
            }
        }
    }

    private fun showStatsSheet(counts: List<Long>) {
        if (isFinishing || isDestroyed) return
        hideLoading()
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_stats, null)
        val dp = resources.displayMetrics.density

        val visibleCounts = counts.mapIndexed { i, c ->
            if (featureLayers[i].isVisible) c else 0L
        }
        val maxCount = visibleCounts.maxOrNull()?.takeIf { it > 0L } ?: 1L
        val total = visibleCounts.sum()

        val container = sheetView.findViewById<LinearLayout>(R.id.llStatsRows)

        categories.forEachIndexed { index, cat ->
            val count = counts[index]
            val isVisible = featureLayers[index].isVisible

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
            }

            // 上行：圆点 + 类名 + 数量
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val dotSize = (10 * dp).toInt()
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).also {
                    it.marginEnd = (8 * dp).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(cat.color)
                }
            }

            val tvName = TextView(this).apply {
                text = cat.name
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_hint))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvCount = TextView(this).apply {
                text = if (isVisible) (count?.toString() ?: "0") else "隐藏"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isVisible) Color.WHITE else ContextCompat.getColor(this@MainActivity, R.color.text_hint))
            }

            topRow.addView(dot)
            topRow.addView(tvName)
            topRow.addView(tvCount)

            // 比例条（圆角，高度加大）
            val barBg = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (10 * dp).toInt()
                ).also { it.topMargin = (5 * dp).toInt() }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(ContextCompat.getColor(this@MainActivity, R.color.surface_mid))
                    cornerRadius = 5 * dp
                }
                clipToOutline = true
            }
            val barFill = View(this).apply {
                setBackgroundColor(cat.color)
                layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
                val fraction = if (isVisible && count != null && count > 0) {
                    count.toFloat() / maxCount
                } else 0f
                barBg.post {
                    layoutParams = FrameLayout.LayoutParams(
                        (barBg.width * fraction).toInt(),
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            }
            barBg.addView(barFill)

            row.addView(topRow)
            row.addView(barBg)
            container.addView(row)

            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.divider_light))
            })
        }

        sheetView.findViewById<TextView>(R.id.tvStatsTotal).text = "合计：$total 个POI"

        sheetView.findViewById<Button>(R.id.btnStatsRefresh).setOnClickListener {
            dialog.dismiss()
            queryAndShowStats()
        }

        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_deep))
        }
        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun requestLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED) {
            startLocationDisplay()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startLocationDisplay() {
        mapView.locationDisplay.startAsync()
    }

    private fun setupMyLocationButton() {
        findViewById<FloatingActionButton>(R.id.fab_my_location).setOnClickListener {
            if (mapView.locationDisplay.isStarted) {
                val myLocation = mapView.locationDisplay.location?.position
                if (myLocation != null) {
                    mapView.setViewpointCenterAsync(myLocation, 10000.0)
                } else {
                    Toast.makeText(this, "正在获取位置...", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestLocationPermission()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationDisplay()
            } else {
                Toast.makeText(this, "位置权限被拒绝，无法显示实时定位", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading() { loadingCard.visibility = View.VISIBLE }
    private fun hideLoading() { loadingCard.visibility = View.GONE }

    override fun onPause() { super.onPause(); mapView.pause() }
    override fun onResume() { super.onResume(); mapView.resume() }
    override fun onDestroy() { super.onDestroy(); mapView.dispose() }
}
