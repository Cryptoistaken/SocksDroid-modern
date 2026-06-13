package net.typeblog.socks.util;

import android.content.Context;
import android.net.VpnService;

import net.typeblog.socks.R;
import static net.typeblog.socks.util.Constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Routes {
	public static void addRoutes(Context context, VpnService.Builder builder, String name) {
		List<String> routes = new ArrayList<>();
		switch (name) {
			case ROUTE_ALL:
				routes.add("0.0.0.0/0");
				break;
			case ROUTE_CHN:
				routes.addAll(Arrays.asList(context.getResources().getStringArray(R.array.simple_route)));
				break;
			case ROUTE_RU:
				routes.addAll(Arrays.asList(context.getResources().getStringArray(R.array.ru_route)));
				break;
			case ROUTE_RU_CHN:
				routes.addAll(Arrays.asList(context.getResources().getStringArray(R.array.simple_route)));
				routes.addAll(Arrays.asList(context.getResources().getStringArray(R.array.ru_route)));
				break;
			default:
				routes.add("0.0.0.0/0");
				break;
		}
		
		for (String r : routes) {
			String[] cidr = r.split("/");
			
			// Cannot handle 127.0.0.0/8
			if (cidr.length == 2 && !cidr[0].startsWith("127")) {
				try {
					builder.addRoute(cidr[0], Integer.parseInt(cidr[1]));
				} catch (Exception e) {
					// Ignore invalid routes
				}
			}
		}
	}
}
