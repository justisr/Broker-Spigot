# Broker - A Bukkit/Spigot implementation of [BrokerAPI](https://github.com/justisr/BrokerAPI)
The concept of Broker surfaced from the plugin development community's need for a means to reliably communicate item prices to one another as well as indicate when a purchase or sale has transpired.

There are plugins enabling per-world, per-region, per-player and permissible markets with unique and specific prices. There are also plugins creating dynamic markets, where prices fluctuate after every transaction, using their own unique algorithms. Yet there are even more plugins dealing with item hoppers, special pickaxes, auto-sell chests, etc, all either forcing their own single static prices for the items they transact, or depending on a specific shop plugin.

In order to avoid forcing server administrators into using a single static market and maintaining multiple redundant price databases, we've needed an abstraction layer through which all of these plugins may communicate with one another other. Broker is that layer.

However, going beyond what is needed is how we enable development beyond what we could have predicted. Thus, Broker doesn't limit itself to just the ItemStack. Broker can be used to communicate prices and transactions associated with any Java object. In fact, Broker even comes with some default implementation for Permissions.

Use Broker for anything you transact. In order to transact through you, calling plugins merely need to have an instance of the object.

### Broker and Vault
Prior to Vault, plugins wanting to utilize player balances were forced to either hard-code in support for a small number of economy plugins or maintain their own balances. The result being that players would often hold conflicting balances across multiple different plugins, severely limiting the number and types of plugins that could be installed. The creation and adoption of Vault opened the doors to a sea of previously unthinkable economy feature combinations. However, we can do even more.

Vault offers a solution for communicating and manipulating player balances. Broker offers a solution for communicating object associated prices and the purchases and sales of those objects. It's a match made in heaven.

Originally, Broker was pitched as an additional service for Vault to offer. However, with the project having been considered "feature complete" for over five years, understandably, it is no longer open to the inclusion of additional APIs. While including Broker's solution within Vault's suite would have been convenient, being a separate project allows Broker the freedom to provide server owners much more control over which implementations are used, how, and in what order. Install both, and enjoy the synergy of two very different projects, both looking to expand the limits of what's possible within your in-game economy.

## Examples
Below are some examples for how to use the BrokerAPI as a caller and as an implementor.

While ItemStacks are not the only object able to be transacted through Broker, they are certainly the most common use case, so I'll be using them within these examples.

Whether you are interacting with Broker as a caller or an implementor, you will only be interacting with the API, so to include Broker within your project, please follow the instructions on the [BrokerAPI](https://github.com/justisr/BrokerAPI) repository.

### Calling
Remember to wait until all plugins have loaded in and registered their implementations before fetching prices or engaging in transactions. Broker loads its defaults in on the first tick, so the soonest you should be calling is on the second: `Bukkit.getScheduler().runTaskLater(this, () -> performAPICalls(), 2);`

As a caller attempting to buy/sell some in game items, you might create methods similar to the following for initiating transactions:
```java

Optional<TransactionRecord<ItemStack>> buy(Player player, ItemStack item, int volume) {
	Optional<PurchaseMediator<ItemStack>> mediator = BrokerAPI.current().forPurchase(player.getUniqueId(), player.getWorld().getUID(), item);
	if (mediator.isEmpty()) return Optional.empty(); // No broker installed for this data set
	TransactionRecord<ItemStack> transaction = mediator.get().buy(volume); // runs the pre-transaction event
	if (!transaction.isSuccess()) return Optional.of(transaction); // return the failed transaction
	withdraw(player, transaction.value()); // <-- your own code
	giveItems(player, item, transaction.volume()); // <-- your own code
	transaction.complete(); // runs the post-transaction event and informs the Broker of the transaction's success
	return Optional.of(transaction); // return successful transaction, after having completed
}

Optional<TransactionRecord<ItemStack>> sell(Player player, ItemStack item, int volume) {
	Optional<SaleMediator<ItemStack>> mediator = BrokerAPI.current().forSale(player.getUniqueId(), player.getWorld().getUID(), item);
	if (mediator.isEmpty()) return Optional.empty(); // No broker installed for this data set
	TransactionRecord<ItemStack> transaction = mediator.get().sell(volume); // runs the pre-transaction event
	if (!transaction.isSuccess()) return Optional.of(transaction); // return the failed transaction
	deposit(player, transaction.value()); // <-- your own code
	takeItems(player, item, transaction.volume()); // <-- your own code
	transaction.complete(); // runs the post-transaction event and informs the Broker of the transaction's success
	return Optional.of(transaction); // return successful transaction, after having completed
}

```
It's important to note that the Broker implementation does **not** handle depositing/withdrawing player balances or removing/providing the items to the player. This is in order to ensure that the functionality that callers may offer is not being shortsightedly limited. For instance, we wouldn't want the Broker to be hastily attempting to remove items from a player's inventory when actually, the item was automatically sold by a special kind of hopper or pickaxe or quarry, etc. Thus, callers must handle balance and inventory management on their end.

Moreover, objects which contain "amount" metadata, such as ItemStacks, will have that amount ignored by the implementing Broker. Volume should be denotated by the volume param.

### Implementing
So you've got some prices for things that you'd like to make available to other plugins? Maybe even regional or permissible markets or some sort of algorithm for balancing prices?

Taking advantage of this API is as simple as implementing the Broker interface and registering it with the current instance of the API.

```java
BrokerAPI.current().register(yourBrokerImpl);
```
Just don't forget to unregister when your plugin disables.

If you need help with filling out your implementation, check out the defaults for some practical examples. Most importantly, remember that Brokers do not handle balance depositing/withdrawing or providing/removing the transacted object. The calling layer does that. Moreover, "amount" metadata, which may be present in objects such as ItemStacks, should be ignored when determining value. Volume should be denotated by the volume param.

Only handle sales or only handles purchases? That's completely fine. Only handle specific ItemStacks like spawners or crops or ores? That's also fine. Simply indicate what you do and don't handle through the "handles" methods, and everything else will be passed on to the next priority Broker.

Do not assume that because the buy or sell methods were run, that the transaction will complete. Even after returning your TransactionRecord, the transaction can still be cancelled by the caller or by a 3rd party listening on the `TransactionPreProcessEvent`. Any calls that assume the transaction has completed should be made within a runnable passed on:

```
TransactionRecordBuilder#buildSuccess(Runnable)
```

This runnable will only be called once the transaction has been completed by the caller, and will only be run once. Use this for adjusting prices, logging transactions, playing sounds, etc.

#### Implementors should also be callers!!
All plugins providing an implementation should **also** call the API for all price and transaction needs. Going through Broker's abstraction layer ensures that if the server administrator has configured another implementation to handle transactions and price management, your plugin will respect that.

Not calling **severely** limits your compatibility with other plugins that provide an implementation, for no benefit to your software's users.

To be a caller and an implementor, simply ensure that you're only interacting with your prices and transaction handlers through the API, so that the server administrator can prioritize a different plugin if they'd prefer that certain purchases and sales which your plugin facilitates be handled by a 3rd party.

## Contributing
If you are the maintainer of a project and you would like to be supported, please provide your implementation within your own project so that Broker does not also need to update whenever your implementation changes. Feel free to create an issue if you need help.

If you've provided an implementation within your own project which has a default implementation here, or if you would not like to be supported by Broker for some reason, please create an issue to request its removal from the project's defaults.

If you would like to transact an object that requires a standardized wrapper for providing additional required data, please create an issue to propose it and detail your use case before creating a PR.

### Building
Because some default implementations are for paid and or closed source projects, default plugin libraries cannot and will not be provided.
Build files will thus vary depending on what plugins are being worked on by any given contributor, so it is asked that they be excluded from all PRs.

If you would like to be a contributor, it is recommended that you add the defaults package to your .gitignore and delete it from your local copy. Changes to the rest of the project may then be compiled, tested, and submitted via PR without issue.

For shading bstats and the API into your compiled jar, add them as dependencies.
```
<dependency>
	<groupId>org.bstats</groupId>
	<artifactId>bstats-bukkit</artifactId>
	<version>2.2.1</version>
	<scope>compile</scope>
</dependency>
<dependency>
	<groupId>com.github.justisr</groupId>
	<artifactId>BrokerAPI</artifactId>
	<version>master-SNAPSHOT</version>
	<scope>compile</scope>
</dependency>
```
Then include those dependencies in your shade plugin configuration.
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-shade-plugin</artifactId>
	<version>3.2.1</version>
	<configuration>
		<artifactSet>
			<includes>
				<include>com.github.justisr:BrokerAPI</include>
				<include>org.bstats:bstats-bukkit</include>
				<include>org.bstats:bstats-base</include>
			</includes>
		</artifactSet>
		<relocations>
			<relocation>
				<pattern>org.bstats</pattern>
				<shadedPattern>com.gmail.justisroot.broker.bstats</shadedPattern>
			</relocation>
		</relocations>
	</configuration>
	<executions>
		<execution>
			<phase>package</phase>
			<goals>
				<goal>shade</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

## License
Copyright (C) 2020 Justis Root justis.root@gmail.com
([MIT License](https://choosealicense.com/licenses/mit/))

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.