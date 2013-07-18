require 'fiber'
require 'irb'

class Ring

  def initialize(*items)
    @items = items.flatten
    @fiber = Fiber.new do
      while true
        Fiber.yield(@items.shift)
      end
    end
  end

  def inspect
    "<##{object_id} @items=#{@items.inspect}>"
  end

  def read
    @fiber.resume
  end

  def <<(item)
    @items << item
  end

end

$ring = Ring.new(1, 2, 3, 4)
while x = $ring.read
  puts x.inspect
end

IRB.start

