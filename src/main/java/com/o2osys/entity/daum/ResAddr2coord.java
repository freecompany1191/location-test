package com.o2osys.entity.daum;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ResAddr2coord {
	@JsonProperty("channel")
	private Channel channel;

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Channel {
		@JsonProperty("totalCount")
		private String totalCount;

		@JsonProperty("link")
		private String link;

		@JsonProperty("result")
		private String result;

		@JsonProperty("generator")
		private String generator;

		@JsonProperty("pageCount")
		private String pageCount;

		@JsonProperty("lastBuildDate")
		private String lastBuildDate;

		@JsonProperty("item")
		private ArrayList<Item> items;

		@JsonProperty("title")
		private String title;

		@JsonProperty("description")
		private String description;

		@Data
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Item {
			@JsonProperty("mountain")
			private String mountain;

			@JsonProperty("mainAddress")
			private String mainAddress;

			@JsonProperty("point_wx")
			private String pointWx;

			@JsonProperty("point_wy")
			private String pointWy;

			@JsonProperty("isNewAddress")
			private String isNewAddress;

			@JsonProperty("buildingAddress")
			private String buildingAddress;

			@JsonProperty("title")
			private String title;

			@JsonProperty("placeName")
			private String placeName;

			@JsonProperty("zipcode")
			private String zipcode;

			@JsonProperty("newAddress")
			private String newAddress;

			@JsonProperty("localName_1")
			private String localName1;

			@JsonProperty("localName_2")
			private String localName2;

			@JsonProperty("localName_3")
			private String localName3;

			@JsonProperty("lat")
			private double lat;

			@JsonProperty("lng")
			private double lng;

			@JsonProperty("point_x")
			private double pointX;

			@JsonProperty("point_y")
			private double pointY;

			@JsonProperty("zone_no")
			private String zoneNo;

			@JsonProperty("subAddress")
			private String subAddress;

			@JsonProperty("id")
			private String id;
		}
	}
}
