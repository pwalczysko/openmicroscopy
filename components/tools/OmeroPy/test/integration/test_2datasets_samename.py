import library as lib


class TestDatasets(lib.ITest):

    def testSameName(self):
        client = self.new_client()
        update = client.sf.getUpdateService()
        dataset1 = self.new_dataset()
        dataset1 = update.saveAndReturnObject(dataset1)
        dataset2 = self.new_dataset(name=dataset1.getName())
        dataset2 = update.saveAndReturnObject(dataset2)
        print dataset1.id.val
        print dataset2.id.val
        print dataset1.name.val
        print dataset2.name.val
        assert dataset1.id.val != dataset2.id.val
        assert dataset1.name.val == dataset2.name.val
