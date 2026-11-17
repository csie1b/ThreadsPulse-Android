package io.github.csie1b.threadspulse.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.csie1b.threadspulse.R
import io.github.csie1b.threadspulse.data.UrlCleaner
import io.github.csie1b.threadspulse.databinding.FragmentDownloaderBinding
import io.github.csie1b.threadspulse.downloader.MediaDownloadManager
import io.github.csie1b.threadspulse.downloader.ThreadPost
import io.github.csie1b.threadspulse.downloader.ThreadsMediaExtractor
import kotlinx.coroutines.launch

class DownloaderFragment : Fragment() {

    private var _binding: FragmentDownloaderBinding? = null
    private val binding get() = _binding!!
    private var currentPost: ThreadPost? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Paste button
        binding.btnPaste.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                val cleaned = UrlCleaner.cleanString(text)
                binding.etUrl.setText(cleaned)
            }
        }

        // Fetch button
        binding.btnFetch.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim() ?: ""
            if (url.isNotEmpty()) {
                fetchPost(url)
            } else {
                Toast.makeText(requireContext(), getString(R.string.downloader_input_hint), Toast.LENGTH_SHORT).show()
            }
        }

        // Download All button
        binding.btnDownloadAll.setOnClickListener {
            val post = currentPost ?: return@setOnClickListener
            downloadMedia(post)
        }

        // Check if preloaded URL exists in arguments
        arguments?.getString("EXTRA_URL")?.let { preloadedUrl ->
            binding.etUrl.setText(preloadedUrl)
            fetchPost(preloadedUrl)
        }
    }

    fun setUrlAndFetch(url: String) {
        binding.etUrl.setText(url)
        fetchPost(url)
    }

    private fun fetchPost(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE

        lifecycleScope.launch {
            val result = ThreadsMediaExtractor.extract(url)
            binding.progressBar.visibility = View.GONE

            result.onSuccess { post ->
                currentPost = post
                displayPost(post)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.downloader_error, error.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayPost(post: ThreadPost) {
        binding.cardResult.visibility = View.VISIBLE
        binding.tvAuthor.text = "@${post.author}"
        binding.tvCaption.text = post.caption.ifEmpty { "(No caption)" }

        val imageCount = post.mediaItems.count { !it.isVideo }
        val videoCount = post.mediaItems.count { it.isVideo }
        val mediaText = StringBuilder()
        if (imageCount > 0) mediaText.append("🖼️ $imageCount 圖片  ")
        if (videoCount > 0) mediaText.append("🎬 $videoCount 影片")

        binding.tvMediaInfo.text = mediaText.toString()
        binding.btnDownloadAll.text = getString(R.string.downloader_download_all, post.mediaItems.size)
        binding.downloadProgress.visibility = View.GONE
    }

    private fun downloadMedia(post: ThreadPost) {
        binding.downloadProgress.visibility = View.VISIBLE
        binding.downloadProgress.max = post.mediaItems.size
        binding.downloadProgress.progress = 0
        binding.btnDownloadAll.isEnabled = false

        lifecycleScope.launch {
            val count = MediaDownloadManager.downloadItems(requireContext(), post.mediaItems) { completed, total ->
                binding.downloadProgress.progress = completed
            }
            binding.btnDownloadAll.isEnabled = true
            binding.downloadProgress.visibility = View.GONE

            Toast.makeText(requireContext(), getString(R.string.downloader_success), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
