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

// Grid adapter for image thumbnails with click/long-click callbacks.
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {
    public interface OnClick { void onItemClick(int pos, IO1.VFile file); }
    public interface OnLong  { void onItemLongClick(int pos, IO1.VFile file); }

    private final Context ctx;
    private final List<IO1.VFile> items = new ArrayList<>();
    private final ExecutorService thumbWork = Executors.newFixedThreadPool(6);
    private OnClick tapCB;
    private OnLong holdCB;
    private int colWidth = 0;

    public ImageAdapter(Context ctx) { this.ctx = ctx; }
    public void setItems(List<IO1.VFile> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }
    public void setOnItemClickListener(OnClick cb) { this.tapCB = cb; }
    public void setOnItemLongClickListener(OnLong cb) { this.holdCB = cb; }

    // Calculate column width after RecyclerView layout.
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        rv.post(() -> {
            int inner = rv.getWidth() - rv.getPaddingLeft() - rv.getPaddingRight();
            if (inner > 0)
                colWidth = inner / 3;
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_image, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        IO1.VFile file = items.get(pos);
        String name = file.GetName(ctx);
        h.txtName.setText(name);

        // Set square thumbnail size
        if (colWidth > 0) {
            int pad = h.itemView.getPaddingLeft() + h.itemView.getPaddingRight();
            int size = colWidth - pad;
            ViewGroup.LayoutParams lp = h.imgThumb.getLayoutParams();
            if (lp.height != size) {
                lp.height = size;
                h.imgThumb.setLayoutParams(lp);
            }
        } else {
            h.itemView.post(() -> {
                int w = h.itemView.getWidth();
                int pad = h.itemView.getPaddingLeft() + h.itemView.getPaddingRight();
                int size = w - pad;
                if (size > 0) {
                    ViewGroup.LayoutParams lp = h.imgThumb.getLayoutParams();
                    if (lp.height != size) {
                        lp.height = size;
                        h.imgThumb.setLayoutParams(lp);
                    }
                }
            });
        }

        // Reset placeholder
        h.imgThumb.setImageBitmap(null);
        h.imgThumb.setBackgroundColor(0x20808080);

        // Load thumbnail asynchronously
        final int idx = pos;
        thumbWork.submit(() -> {
            try {
                Bitmap thumb = decodeThumb(file, 256);
                if (thumb != null && h.getBindingAdapterPosition() == idx) {
                    h.imgThumb.post(() -> {
                        h.imgThumb.setImageBitmap(thumb);
                        h.imgThumb.setBackgroundColor(0);
                    });
                }
            } catch (Exception e) {
                /* ignore */ }
        });

        // Tap to view
        h.itemView.setOnClickListener(v -> {
            int bindingPos = h.getBindingAdapterPosition();
            if (tapCB != null && bindingPos != RecyclerView.NO_POSITION) {
                tapCB.onItemClick(bindingPos, items.get(bindingPos));
            }
        });

        // Hold to delete
        h.itemView.setOnLongClickListener(v -> {
            int bindingPos = h.getBindingAdapterPosition();
            if (holdCB != null && bindingPos != RecyclerView.NO_POSITION) {
                holdCB.onItemLongClick(bindingPos, items.get(bindingPos));
            }
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    // Decode thumbnail with inSampleSize, applying EXIF rotation.
    private Bitmap decodeThumb(IO1.VFile file, int maxSize) {
        try {
            int rot = Core.exifRotation(ctx, file);

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            try (InputStream is = file.OpenReader(ctx)) { BitmapFactory.decodeStream(is, null, opt); }

            int sample = 1;
            while (opt.outWidth / sample > maxSize || opt.outHeight / sample > maxSize) sample *= 2;

            opt.inJustDecodeBounds = false;
            opt.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream is = file.OpenReader(ctx)) { bmp = BitmapFactory.decodeStream(is, null, opt); }
            return Core.applyRotation(bmp, rot);
        } catch (Exception e) {
            return null;
        }
    }

    public void shutdown() { thumbWork.shutdown(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView txtName;

        VH(View v) {
            super(v);
            imgThumb = v.findViewById(R.id.imgThumb);
            txtName = v.findViewById(R.id.txtName);
        }
    }
}
