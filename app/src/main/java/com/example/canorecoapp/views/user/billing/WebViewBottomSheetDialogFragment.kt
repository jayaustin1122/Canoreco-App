import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.utils.DialogUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WebViewBottomSheetDialogFragment(private val url: String) : BottomSheetDialogFragment() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_web_view_bottom_sheet_dialog, container, false)

        val webView = view.findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        DialogUtils.showLoading(requireActivity()).dismiss()
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            behavior.isDraggable = false // Prevent dragging to close the dialog
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT // Set fixed height
        }
        DialogUtils.showLoading(requireActivity()).dismiss()
        return dialog
    }
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        val bundle = Bundle().apply {
            putInt("selectedFragmentId", R.id.navigation_services)
        }

        findNavController().navigate(R.id.userHolderFragment, bundle)
    }
}
