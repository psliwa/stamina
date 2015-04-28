package stamina
package avro

class AvroPersisterSpec extends StaminaAvroSpec {
  import AvroTestDomain._

  val v1CartCreatedPersister = persister[CartCreatedV1]("cart-created")

  // val v2CartCreatedPersister = persister[CartCreatedV2, V2]("cart-created",
  //   from[V1].to[V2](_.update('cart / 'items / * / 'price ! set[Int](1000)))
  // )

  // val v3CartCreatedPersister = persister[CartCreatedV3, V3]("cart-created",
  //   from[V1]
  //     .to[V2](_.update('cart / 'items / * / 'price ! set[Int](1000)))
  //     .to[V3](_.update('timestamp ! set[Long](System.currentTimeMillis - 3600000L)))
  // )

  "V1 persisters produced by SprayJsonPersister" should {
    // "correctly persist and unpersist domain events " in {
    //   import v1CartCreatedPersister._
    //   unpersist(persist(v1CartCreated)) should equal(v1CartCreated)
    // }
  }

  "A hand-coded Avro schema for CartCreatedV1" should {
    "work" in {

      import org.apache.avro._
      import org.apache.avro.generic._
      import org.apache.avro.io._

      // case class ItemV1(id: ItemId, name: String)
      // case class CartV1(id: CartId, items: List[ItemV1])
      // case class CartCreatedV1(cart: CartV1)

      // format: OFF
      val schema: Schema = SchemaBuilder.record("r")
        .fields
          .name("cart").`type`.record("cart")
            .fields
              .name("id").`type`.intType.noDefault
              .name("items").`type`.array.items.record("item")
                .fields
                  .name("id").`type`.intType.noDefault
                  .name("name").`type`.stringType.noDefault
              .endRecord
              .noDefault
          .endRecord
          .noDefault
        .endRecord
      // format: ON

      val cartSchema = schema.getField("cart").schema()
      val itemsSchema = cartSchema.getField("items").schema()
      val itemSchema = itemsSchema.getElementType()

      // println("----------------------------------------------------------------------------------------->")
      // println(itemSchema.toString(true))
      // println("----------------------------------------------------------------------------------------->")
      // println(itemsSchema.toString(true))
      // println("----------------------------------------------------------------------------------------->")
      // println(cartSchema.toString(true))
      // println("----------------------------------------------------------------------------------------->")
      // println(schema.toString(true))
      // println("----------------------------------------------------------------------------------------->")

      val itemRecords = v1CartCreated.cart.items.map { item ⇒
        new GenericRecordBuilder(itemSchema).
          set("id", item.id).
          set("name", item.name).
          build()
      }

      import scala.collection.JavaConversions._

      val cart: GenericRecord = new GenericData.Record(cartSchema)

      cart.put("id", v1CartCreated.cart.id)
      cart.put("items", new GenericData.Array[GenericData.Record](itemsSchema, itemRecords))

      val cartCreated: GenericRecord = new GenericData.Record(schema)

      cartCreated.put("cart", cart)

      val out = new java.io.ByteArrayOutputStream
      val datumWriter = new GenericDatumWriter[GenericRecord](schema)
      val encoder = EncoderFactory.get.jsonEncoder(schema, out, true)

      datumWriter.write(cartCreated, encoder)
      encoder.flush

      println("----------------------------------------------------------------------------------------->")
      println("Encoded as JSON:")
      println(out.toString("UTF-8"))
      println("----------------------------------------------------------------------------------------->")
    }
  }

  // "V2 persisters with migrators produced by SprayJsonPersister" should {
  //   "correctly persist and unpersist domain events " in {
  //     import v2CartCreatedPersister._
  //     unpersist(persist(v2CartCreated)) should equal(v2CartCreated)
  //   }

  //   "correctly migrate and unpersist V1 domain events" in {
  //     val v1Persisted = v1CartCreatedPersister.persist(v1CartCreated)
  //     val v2Unpersisted = v2CartCreatedPersister.unpersist(v1Persisted)

  //     v2Unpersisted.cart.items.map(_.price).toSet should equal(Set(1000))
  //   }
  // }

  // "V3 persisters with migrators produced by SprayJsonPersister" should {
  //   "correctly persist and unpersist domain events " in {
  //     import v3CartCreatedPersister._
  //     unpersist(persist(v3CartCreated)) should equal(v3CartCreated)
  //   }

  //   "correctly migrate and unpersist V1 domain events" in {
  //     val v1Persisted = v1CartCreatedPersister.persist(v1CartCreated)
  //     val v2Persisted = v2CartCreatedPersister.persist(v2CartCreated)

  //     val v1Unpersisted = v3CartCreatedPersister.unpersist(v1Persisted)
  //     val v2Unpersisted = v3CartCreatedPersister.unpersist(v2Persisted)

  //     v1Unpersisted.cart.items.map(_.price).toSet should equal(Set(1000))
  //     v2Unpersisted.timestamp should (be > 0L and be < System.currentTimeMillis)
  //   }
  // }
}
