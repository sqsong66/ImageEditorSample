#ifndef _LIB_ENHANCE_FOREGROUND_H_
#define _LIB_ENHANCE_FOREGROUND_H_

#include "opencv2/opencv.hpp"

class enhance_foreground {
private:
	static int prepare_rgb_data(cv::Mat& image, uint8_t * img_src_data, int img_w, int img_h, int src_type, int src_stride) {
		if (src_type != 3 && src_type != 4 && src_type != 1)
			return -1;

		int src_row_index = 0, src_index = 0, img_index = 0;
		float temp = 0.0f;
		for (int i = 0; i < img_h; i++) {
			float* image_buf = image.ptr<float>(i);
			img_index = 0;
			for (int j = 0; j < img_w; j++) {
				if (src_type == 1) {
					temp = img_src_data[src_index] / 255.0f;
					image_buf[img_index] = temp;
					image_buf[img_index + 1] = temp;
					image_buf[img_index + 2] = temp;
				}
				else {
                    float alpha = img_src_data[src_index + 3] / 255.0f;
                    if (alpha != 0.0f) {
                        image_buf[img_index] = img_src_data[src_index] / 255.0f / alpha;
                        image_buf[img_index + 1] = img_src_data[src_index + 1] / 255.0f / alpha;
                        image_buf[img_index + 2] = img_src_data[src_index + 2] / 255.0f / alpha;
                    } else {
                        image_buf[img_index] = img_src_data[src_index] / 255.0f;
                        image_buf[img_index + 1] = img_src_data[src_index + 1] / 255.0f;
                        image_buf[img_index + 2] = img_src_data[src_index + 2] / 255.0f;
                    }
				}
				img_index += 3;
				src_index += src_type;
			}
			src_row_index += src_stride;
			src_index = src_row_index;
		}
		return 0;
	}

	static int prepare_alpha_data(cv::Mat& alpha, uint8_t * alpha_src_data, int img_w, int img_h, int src_alpha_type,int alpha_stride,
                                  int& min_x, int& min_y, int& max_x, int& max_y,int& white_count, bool is_mask_alpha_channel) {
		int src_row_index = 0, src_index = 0, alpha_index = 0;
		white_count = 0;
		float temp = 0.f;
		min_x = img_w;
		min_y = img_h;
		max_x = -1;
		max_y = -1;
		for (int i = 0; i < img_h; i++) {
			float* alpha_buf = alpha.ptr<float>(i);
			alpha_index = 0;
			for (int j = 0; j < img_w; j++) {
                int alpha_data = is_mask_alpha_channel ? alpha_src_data[src_index + 3] : alpha_src_data[src_index];
				if (alpha_data > 0) {
					white_count++;
					if (i < min_y)
						min_y = i;
					if (i > max_y)
						max_y = i;
					if (j < min_x)
						min_x = j;
					if (j > max_x)
						max_x = j;

					temp = alpha_data / 255.0f;

					alpha_buf[alpha_index] = temp;
					alpha_buf[alpha_index + 1] = temp;
					alpha_buf[alpha_index + 2] = temp;
				}
				src_index += src_alpha_type;
				alpha_index += 3;
			}
			src_row_index += alpha_stride;
			src_index = src_row_index;
		}
		return 0;
	}

	static uint8_t clip(float f) {
		uint8_t r = 0;
		if (f > 255.0f)
			r = 255;
		else if (f < 0.0f)
			r = 0;
		else
			r = (uint8_t)f;
		return r;
	}

	static int mat_div(cv::Mat& src1, cv::Mat& src2, cv::Mat& dst, float gamma, int img_w, int img_h) {
		int img_index = 0;
		float tmp = 0.0f;
		for (int i = 0; i < img_h; i++) {
			float* src1_buf = src1.ptr<float>(i);
			float* src2_buf = src2.ptr<float>(i);
			float* dst_buf = dst.ptr<float>(i);
			img_index = 0;
			for (int j = 0; j < img_w; j++) {
				dst_buf[img_index] = src1_buf[img_index] / (src2_buf[img_index] + gamma);
				img_index += 1;
				dst_buf[img_index] = src1_buf[img_index] / (src2_buf[img_index] + gamma);
				img_index += 1;
				dst_buf[img_index] = src1_buf[img_index] / (src2_buf[img_index] + gamma);
				img_index += 1;
			}
		}
		return 0;
	}

	static int mat_div1(cv::Mat& src1, cv::Mat& src2, cv::Mat& dst, float gamma, int img_w, int img_h) {
		int img_index = 0;
		float tmp = 0.0f;
		for (int i = 0; i < img_h; i++) {
			float* src1_buf = src1.ptr<float>(i);
			float* src2_buf = src2.ptr<float>(i);
			float* dst_buf = dst.ptr<float>(i);
			img_index = 0;
			for (int j = 0; j < img_w; j++) {
				dst_buf[img_index] = src1_buf[img_index] / (gamma - src2_buf[img_index]);
				img_index += 1;
				dst_buf[img_index] = src1_buf[img_index] / (gamma - src2_buf[img_index]);
				img_index += 1;
				dst_buf[img_index] = src1_buf[img_index] / (gamma - src2_buf[img_index]);
				img_index += 1;
			}
		}
		return 0;
	}

	static int save_result(cv::Mat& img_fg, uint8_t * img_dst, int img_w, int img_h, int dst_type, int dst_stride, int min_x, int min_y, int max_x, int max_y) {
		int dst_row_index = dst_stride*min_y, dst_index = dst_row_index, img_index = 0;
		int st_img = 3 * min_x;
		int st_dst = min_x*dst_type;
		float temp = 0.0f;
		if (max_x - min_x < img_w || max_y - min_y < img_h) {
			memset(img_dst, (uint8_t)0, sizeof(uint8_t)*dst_stride*img_h);
		}
		for (int i = min_y; i < max_y; i++) {
			float* image_buf = img_fg.ptr<float>(i);
			img_index = st_img;
			dst_index += st_dst;
			for (int j = min_x; j < max_x; j++) {
				if (dst_type == 1) {
					img_dst[dst_index] = clip(image_buf[img_index] * 255.0f);
				}
				else {
					img_dst[dst_index] = clip(image_buf[img_index] * 255.0f);
					img_dst[dst_index + 1] = clip(image_buf[img_index + 1] * 255.0f);
					img_dst[dst_index + 2] = clip(image_buf[img_index + 2] * 255.0f);
					img_dst[dst_index + 3] = 255;
				}

				img_index += 3;
				dst_index += dst_type;
			}
			dst_row_index += dst_stride;
			dst_index = dst_row_index;
		}
		return 0;
	}

	static void set_margin(int& min_x, int& min_y, int& max_x, int& max_y, int margin, int img_width, int img_height) {
		min_x = min_x - margin;
		min_y = min_y - margin;
		max_x = max_x + margin;
		max_y = max_y + margin;
		if (min_x < 0)
			min_x = 0;
		if (min_y < 0)
			min_y = 0;
		if (max_x > img_width - 1)
			max_x = img_width - 1;
		if (max_y > img_height - 1)
			max_y = img_height - 1;
	}

public:
	static int enhance(uint8_t * img_dst, int dst_type, int dst_stride, uint8_t * img_src, int img_width, int img_height, int src_type, int src_stride,
                       uint8_t * alpha_src, int alpha_type, int alpha_stride,int *rect, bool is_mask_alpha_channel) {
		cv::Mat alpha = cv::Mat::zeros(img_height, img_width, CV_32FC3);
		if (alpha.empty())
			return -6;
		int min_x, min_y, max_x, max_y, white_count;
		prepare_alpha_data(alpha, alpha_src, img_width, img_height, alpha_type, alpha_stride, min_x,
						   min_y, max_x, max_y, white_count, is_mask_alpha_channel);

		if (rect != NULL) {
			if (max_x <= min_x || max_y <= min_y) {
				rect[0] = 0;
				rect[1] = 0;
				rect[2] = 0;
				rect[3] = 0;
			} else {
				rect[0] = min_x;
				rect[1] = min_y;
				rect[2] = max_x - min_x + 1;
				rect[3] = max_y - min_y + 1;
			}
		}

		if ((max_x - min_x) <= 5 || (max_y - min_y) <= 5)//???????
			return -7;

		float sw1 = (float) (img_width * img_height) / ((max_x - min_x + 1) * (max_y - min_y + 1));
		float sw2 = (float) (img_width * img_height - white_count) / white_count;

		if (sw2 > 8) { //?????????????????????�� ??????
			return -8;
		}
		if (sw2 < 0.06f)//???????
			return -9;

		cv::Mat image = cv::Mat(img_height, img_width, CV_32FC3);
		if (image.empty())
			return -6;
		prepare_rgb_data(image, img_src, img_width, img_height, src_type, src_stride);


		cv::Mat img_fg;
		int const_size = 600;
		int max_ = (img_width > img_height ? img_width : img_height);
		if (max_ > const_size) {
			cv::Mat image_s;
			cv::Mat alpha_s;
			int w = 0, h = 0;
			cv::Mat image_roi;
			cv::Mat alpha_roi;
			//cv::Rect roi;
			int roi_w = 0, roi_h = 0;

			if (sw1 < 1.2f) {
				//???roi
				if (img_width == max_) {
					h = const_size * img_height / img_width;
					w = const_size;
				} else {
					w = const_size * img_width / img_height;
					h = const_size;
				}
				min_x = 0;
				min_y = 0;
				max_x = img_width;
				max_y = img_height;
			} else {
				set_margin(min_x, min_y, max_x, max_y, max_ > 1500 ? 100 : 50, img_width,
						   img_height);
				max_x = max_x + 1;
				max_y = max_y + 1;
				roi_w = max_x - min_x;
				roi_h = max_y - min_y;
				cv::Rect roi(min_x, min_y, roi_w, roi_h);

				image_roi = image(roi);
				alpha_roi = alpha(roi);

				max_ = (roi_w > roi_h ? roi_w : roi_h);
				if (roi_w == max_) {
					h = const_size * roi_h / roi_w;
					w = const_size;
				} else {
					w = const_size * roi_w / roi_h;
					h = const_size;
				}
			}
			cv::Size resize_size(w, h);

			if (image_roi.empty()) {
				cv::resize(image, image_s, resize_size, 0, 0, cv::INTER_AREA);
				cv::resize(alpha, alpha_s, resize_size, 0, 0, cv::INTER_AREA);
			} else {
				cv::resize(image_roi, image_s, resize_size, 0, 0, cv::INTER_AREA);
				cv::resize(alpha_roi, alpha_s, resize_size, 0, 0, cv::INTER_AREA);
			}

			cv::Mat blurred_alpha;
			cv::Size blur_size(11, 11);
			cv::blur(alpha_s, blurred_alpha, blur_size);

			cv::Mat img_alpha;
			cv::multiply(image_s, alpha_s, img_alpha);
			cv::Mat img_asq;
			cv::multiply(img_alpha, alpha_s, img_asq);

			cv::Mat bg_s;
			cv::scaleAdd(img_alpha, -2, image_s, bg_s);
			cv::add(bg_s, img_asq, bg_s);

			cv::Mat blurred_fg_alpha;
			cv::blur(img_asq, blurred_fg_alpha, blur_size);
			cv::Mat blurred_fg_ = cv::Mat(h, w, CV_32FC3);
			mat_div(blurred_fg_alpha, blurred_alpha, blurred_fg_, (float) 1e-5, w, h);

			cv::Mat blurred_bg_alpha;
			cv::blur(bg_s, blurred_bg_alpha, blur_size);
			cv::Mat blurred_bg_ = cv::Mat(h, w, CV_32FC3);
			mat_div(blurred_bg_alpha, blurred_alpha, blurred_bg_, (float) (1 + 1e-5), w, h);

			cv::Mat blurred_fg;
			cv::Mat blurred_bg;

			if (image_roi.empty()) {
				cv::Size img_size(img_width, img_height);
				cv::resize(blurred_fg_, blurred_fg, img_size);
				cv::resize(blurred_bg_, blurred_bg, img_size);
				cv::subtract(blurred_fg, blurred_bg, img_fg);
				cv::multiply(alpha, img_fg, img_fg);
				cv::subtract(image, img_fg, img_fg);
				cv::subtract(img_fg, blurred_bg, img_fg);
				cv::multiply(alpha, img_fg, img_fg);
				cv::add(blurred_fg, img_fg, img_fg);
			} else {
				cv::Size img_size(roi_w, roi_h);
				cv::resize(blurred_fg_, blurred_fg, img_size);
				cv::resize(blurred_bg_, blurred_bg, img_size);

				cv::Mat img_fg_;
				cv::subtract(blurred_fg, blurred_bg, img_fg_);
				cv::multiply(alpha_roi, img_fg_, img_fg_);
				cv::subtract(image_roi, img_fg_, img_fg_);
				cv::subtract(img_fg_, blurred_bg, img_fg_);
				cv::multiply(alpha_roi, img_fg_, img_fg_);
				cv::add(blurred_fg, img_fg_, img_fg_);

				//img_fg = cv::Mat::zeros(img_height, img_width, CV_32FC3);

				//cv::Mat img_fg_roi = img_fg(roi);
				//img_fg_.copyTo(img_fg_roi);
				img_fg_.copyTo(image_roi);
				img_fg = image;
			}
		} else {
			cv::Mat img_alpha;
			cv::multiply(image, alpha, img_alpha);
			cv::Mat img_asq;
			cv::multiply(img_alpha, alpha, img_asq);

			cv::Mat img_bg;
			cv::scaleAdd(img_alpha, -2, image, img_bg);
			cv::add(img_bg, img_asq, img_bg);

			cv::Mat blurred_alpha;
			cv::Size blur_size(11, 11);
			cv::blur(alpha, blurred_alpha, blur_size);

			cv::Mat blurred_fg_alpha;
			cv::blur(img_asq, blurred_fg_alpha, blur_size);
			cv::Mat blurred_fg = cv::Mat(img_height, img_width, CV_32FC3);
			mat_div(blurred_fg_alpha, blurred_alpha, blurred_fg, (float) 1e-5, img_width,
					img_height);

			cv::Mat blurred_bg_alpha;
			cv::blur(img_bg, blurred_bg_alpha, blur_size);
			cv::Mat blurred_bg = cv::Mat(img_height, img_width, CV_32FC3);
			mat_div1(blurred_bg_alpha, blurred_alpha, blurred_bg, (float) (1 + 1e-5), img_width,
					 img_height);

			cv::subtract(blurred_fg, blurred_bg, img_fg);
			cv::multiply(alpha, img_fg, img_fg);
			cv::subtract(image, img_fg, img_fg);
			cv::subtract(img_fg, blurred_bg, img_fg);
			cv::multiply(alpha, img_fg, img_fg);
			cv::add(blurred_fg, img_fg, img_fg);
			min_x = 0;
			min_y = 0;
			max_x = img_width;
			max_y = img_height;
		}

		save_result(img_fg, img_dst, img_width, img_height, dst_type, dst_stride, min_x, min_y,
					max_x, max_y);
		return 0;
	}
};

#endif