package com.mojang.realmsclient.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class RealmsServerList {
	private final Minecraft minecraft;
	private final Set<RealmsServer> removedServers = Sets.<RealmsServer>newHashSet();
	private List<RealmsServer> servers = Lists.<RealmsServer>newArrayList();

	public RealmsServerList(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	public List<RealmsServer> updateServersList(List<RealmsServer> list) {
		List<RealmsServer> list2 = new ArrayList(list);
		list2.sort(new RealmsServer.McoServerComparator(this.minecraft.getUser().getName()));
		boolean bl = list2.removeAll(this.removedServers);
		if (!bl) {
			this.removedServers.clear();
		}

		this.servers = list2;
		return List.copyOf(this.servers);
	}

	public synchronized List<RealmsServer> removeItem(RealmsServer realmsServer) {
		this.servers.remove(realmsServer);
		this.removedServers.add(realmsServer);
		return List.copyOf(this.servers);
	}
}
