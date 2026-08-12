package com.example.k7ebookificate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RecyclerView Adapter for displaying image thumbnails in a grid.
 * Loads thumbnails in background threads with inSampleSize for memory efficiency.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, IO1.VFile file);
    }

    private final Context context;
    private final List<IO1.VFile> items = new ArrayList<>();
    private final ExecutorService thumbExecutor = Executors.newFixedThreadPool(3);
    private OnItemLongClickListener longClickListener;

    public ImageAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<IO1.VFile> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IO1.VFile file = items.get(position);
        String name = file.GetName(context);
        holder.txtName.setText(name);

        // Make thumbnail square based on view width
        holder.imgThumb.post(() -> {
            int width = holder.imgThumb.getWidth();
            if (width > 0) {
                ViewGroup.LayoutParams params = holder.imgThumb.getLayoutParams();
                params.height = width;
                holder.imgThumb.setLayoutParams(params);
            }
        });

        // Reset image while loading
        holder.imgThumb.setImageBitmap(null);
        holder.imgThumb.setBackgroundColor(0x20808080);

        // Load thumbnail in background
        final int pos = position;
        thumbExecutor.submit(() -> {
            try {
                Bitmap thumb = loadThumbnail(file, 256);
                if (thumb != null && holder.getAdapterPosition() == pos) {
                    holder.imgThumb.post(() -> {
                        holder.imgThumb.setImageBitmap(thumb);
                        holder.imgThumb.setBackgroundColor(0);
                    });
                }
            } catch (Exception e) { /* ignore */ }
        });

        // Long click for delete
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position, file);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private Bitmap loadThumbnail(IO1.VFile file, int targetSize) {
        try {
            // First pass: get dimensions
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream is = file.OpenReader(context)) {
                BitmapFactory.decodeStream(is, null, opts);
            }

            // Calculate inSampleSize
            int w = opts.outWidth;
            int h = opts.outHeight;
            int inSampleSize = 1;
            while (w / inSampleSize > targetSize || h / inSampleSize > targetSize) {
                inSampleSize *= 2;
            }

            // Second pass: decode with sample size
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;
            try (InputStream is = file.OpenReader(context)) {
                return BitmapFactory.decodeStream(is, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void shutdown() {
        thumbExecutor.shutdown();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView txtName;

        ViewHolder(View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            txtName = itemView.findViewById(R.id.txtName);
        }
    }
}
