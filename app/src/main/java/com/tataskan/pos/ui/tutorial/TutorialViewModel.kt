package com.tataskan.pos.ui.tutorial

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TutorialViewModel : ViewModel() {
    private val _currentStepIndex = MutableStateFlow(-1)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _isWelcomeDismissed = MutableStateFlow(false)
    val isWelcomeDismissed: StateFlow<Boolean> = _isWelcomeDismissed.asStateFlow()

    private val _targetBounds = MutableStateFlow<Rect?>(null)
    val targetBounds: StateFlow<Rect?> = _targetBounds.asStateFlow()

    val tutorialSteps = listOf(
        TutorialStep("welcome", "tut_welcome_title", "tut_welcome_desc", Alignment.Center, hasTarget = false),
        TutorialStep("pos", "tut_pos_title", "tut_pos_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("scanner_icon", "tut_pos_scan_title", "tut_pos_scan_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("scanner_explain", "tut_scanner_explain_title", "tut_scanner_explain_desc", Alignment.Center, hasTarget = false),
        TutorialStep("checkout_btn", "tut_checkout_btn_title", "tut_checkout_btn_desc", Alignment.TopCenter, requireAction = true),
        TutorialStep("amount_input", "tut_amount_received_title", "tut_amount_received_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("complete_sale_btn", "tut_complete_sale_title", "tut_complete_sale_desc", Alignment.TopCenter, requireAction = true),
        TutorialStep("finish_pos", "tut_finish_title", "tut_finish_desc", Alignment.Center, hasTarget = false),
        TutorialStep("inventory_home", "tut_inventory_home_title", "tut_inventory_home_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("inv_search", "tut_inv_search_title", "tut_inv_search_desc", Alignment.BottomCenter),
        TutorialStep("inv_low_stock", "tut_inv_low_stock_title", "tut_inv_low_stock_desc", Alignment.BottomCenter),
        TutorialStep("inv_filter", "tut_inv_filter_title", "tut_inv_filter_desc", Alignment.BottomCenter),
        TutorialStep("inv_add", "tut_inv_add_title", "tut_inv_add_desc", Alignment.Center, requireAction = true),
        TutorialStep("inv_barcode_gen", "tut_inv_barcode_gen_title", "tut_inv_barcode_gen_desc", Alignment.BottomCenter),
        TutorialStep("inv_save_btn", "tut_inv_save_title", "tut_inv_save_desc", Alignment.TopCenter, requireAction = true),
        TutorialStep("inv_card", "tut_inv_card_title", "tut_inv_card_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("inv_detail", "tut_inv_detail_title", "tut_inv_detail_desc", Alignment.Center),
        TutorialStep("inv_download", "tut_inv_download_title", "tut_inv_download_desc", Alignment.TopCenter),
        TutorialStep("inv_delete", "tut_inv_delete_title", "tut_inv_delete_desc", Alignment.TopCenter),
        TutorialStep("inv_print", "tut_inv_print_title", "tut_inv_print_desc", Alignment.Center),
        TutorialStep("more_nav", "tut_more_nav_title", "tut_more_nav_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("more_reports_item", "tut_more_reports_title", "tut_more_reports_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("reporting_export_btn", "tut_reporting_export_title", "tut_reporting_export_desc", Alignment.TopCenter),
        TutorialStep("more_promos_item", "tut_more_promos_title", "tut_more_promos_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("promo_add_fab", "tut_promo_add_title", "tut_promo_add_desc", Alignment.TopCenter),
        TutorialStep("more_labels_item", "tut_more_labels_title", "tut_more_labels_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("label_gen_btn", "tut_label_gen_title", "tut_label_gen_desc", Alignment.TopCenter),
        TutorialStep("more_settings_item", "tut_more_settings_title", "tut_more_settings_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("settings_business", "tut_settings_business_title", "tut_settings_business_desc", Alignment.BottomCenter),
        TutorialStep("settings_tax", "tut_settings_tax_title", "tut_settings_tax_desc", Alignment.BottomCenter),
        TutorialStep("settings_regional", "tut_settings_regional_title", "tut_settings_regional_desc", Alignment.BottomCenter),
        TutorialStep("settings_backup", "tut_settings_backup_title", "tut_settings_backup_desc", Alignment.BottomCenter),
        TutorialStep("settings_demo", "tut_settings_demo_title", "tut_settings_demo_desc", Alignment.BottomCenter),
        TutorialStep("more_feedback_item", "tut_more_feedback_title", "tut_more_feedback_desc", Alignment.BottomCenter, requireAction = true),
        TutorialStep("feedback_stars", "tut_feedback_stars_title", "tut_feedback_stars_desc", Alignment.BottomCenter),
        TutorialStep("feedback_submit_btn", "tut_feedback_submit_title", "tut_feedback_submit_desc", Alignment.TopCenter),
        TutorialStep("final", "tut_final_title", "tut_final_desc", Alignment.Center, hasTarget = false)
    )

    val activeStep: StateFlow<TutorialStep?> = combine(_currentStepIndex, _isWelcomeDismissed) { index, dismissed ->
        if (dismissed && index in tutorialSteps.indices) {
            tutorialSteps[index]
        } else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setStepIndex(index: Int) {
        _currentStepIndex.value = index
    }

    fun setWelcomeDismissed(dismissed: Boolean) {
        _isWelcomeDismissed.value = dismissed
    }

    fun updateTargetBounds(rect: Rect?) {
        _targetBounds.value = rect
    }

    fun reset() {
        _currentStepIndex.value = 0
        _isWelcomeDismissed.value = true
        _targetBounds.value = null
    }

    fun nextStep() {
        if (_currentStepIndex.value < tutorialSteps.size - 1) {
            _currentStepIndex.value++
        } else {
            _currentStepIndex.value = -1
        }
        _targetBounds.value = null
    }
}
